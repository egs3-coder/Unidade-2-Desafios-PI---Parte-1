package br.com.agendasaude.agendamento;

import br.com.agendasaude.cadastro.Medico;
import br.com.agendasaude.cadastro.MedicoRepository;
import br.com.agendasaude.cadastro.Paciente;
import br.com.agendasaude.cadastro.PacienteRepository;
import br.com.agendasaude.dominio.RecursoNaoEncontradoException;
import br.com.agendasaude.dominio.RegraDeNegocioException;
import br.com.agendasaude.dominio.Violacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * T3, T7 e T11 · Regras que dependem do estado da agenda.
 * O banco é substituído por dublês (Mockito): o teste é unitário, roda em milissegundos e sem Docker.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("T3/T7/T11 · AgendamentoService")
class AgendamentoServiceTest {

    /** Segunda-feira, 19/10/2026, 09:00 em São Paulo (12:00 UTC). */
    private static final Clock RELOGIO =
            Clock.fixed(Instant.parse("2026-10-19T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private static final LocalDateTime TERCA_10H = LocalDateTime.of(2026, 10, 20, 10, 0);
    private static final Long MEDICO_ID = 1L;
    private static final Long PACIENTE_ID = 2L;

    @Mock ConsultaRepository consultas;
    @Mock MedicoRepository medicos;
    @Mock PacienteRepository pacientes;

    private AgendamentoService service;
    private final Medico medico = new Medico("Dra. Ana Lima", "Clínica geral", "CRM-PE 12345");
    private final Paciente paciente = new Paciente("João Souza", "joao@exemplo.com");

    @BeforeEach
    void setUp() {
        service = new AgendamentoService(consultas, medicos, pacientes, RELOGIO);
    }

    private void medicoEPacienteCadastrados() {
        when(medicos.findById(MEDICO_ID)).thenReturn(Optional.of(medico));
        when(pacientes.findById(PACIENTE_ID)).thenReturn(Optional.of(paciente));
    }

    private Violacao violacao(Runnable acao) {
        return assertThrows(RegraDeNegocioException.class, acao::run).getViolacao();
    }

    // ---------- T3 · agendar ----------

    @Test
    @DisplayName("agenda em horário livre e salva com status AGENDADA")
    void agendaHorarioLivre() {
        medicoEPacienteCadastrados();
        when(consultas.save(any(Consulta.class))).thenAnswer(chamada -> chamada.getArgument(0));

        ConsultaResponse resposta = service.agendar(MEDICO_ID, PACIENTE_ID, TERCA_10H);

        assertEquals("AGENDADA", resposta.status());
        assertEquals(TERCA_10H, resposta.inicio());
        assertEquals("Dra. Ana Lima", resposta.medico());
        verify(consultas).save(any(Consulta.class));
    }

    @Test
    @DisplayName("recusa quando o médico já tem consulta no horário e não salva nada")
    void recusaMedicoOcupado() {
        medicoEPacienteCadastrados();
        when(consultas.existsByMedicoIdAndInicioAndStatus(MEDICO_ID, TERCA_10H, StatusConsulta.AGENDADA))
                .thenReturn(true);

        assertEquals(Violacao.MEDICO_OCUPADO, violacao(() -> service.agendar(MEDICO_ID, PACIENTE_ID, TERCA_10H)));
        verify(consultas, never()).save(any(Consulta.class));
    }

    @Test
    @DisplayName("recusa quando o paciente já tem consulta no horário (com outro médico)")
    void recusaPacienteOcupado() {
        medicoEPacienteCadastrados();
        when(consultas.existsByPacienteIdAndInicioAndStatus(PACIENTE_ID, TERCA_10H, StatusConsulta.AGENDADA))
                .thenReturn(true);

        assertEquals(Violacao.PACIENTE_OCUPADO, violacao(() -> service.agendar(MEDICO_ID, PACIENTE_ID, TERCA_10H)));
        verify(consultas, never()).save(any(Consulta.class));
    }

    @Test
    @DisplayName("recusa sábado antes de consultar o banco (regra barata primeiro)")
    void recusaForaDoExpedienteSemTocarNoBanco() {
        LocalDateTime sabado = LocalDateTime.of(2026, 10, 24, 10, 0);

        assertEquals(Violacao.FORA_DO_EXPEDIENTE, violacao(() -> service.agendar(MEDICO_ID, PACIENTE_ID, sabado)));
        verifyNoInteractions(consultas, medicos, pacientes);
    }

    @Test
    @DisplayName("recusa horário de hoje que já passou (segunda 08:00, agora são 09:00)")
    void recusaHorarioPassado() {
        LocalDateTime hojeAsOito = LocalDateTime.of(2026, 10, 19, 8, 0);

        assertEquals(Violacao.HORARIO_NO_PASSADO, violacao(() -> service.agendar(MEDICO_ID, PACIENTE_ID, hojeAsOito)));
    }

    @Test
    @DisplayName("responde 'não encontrado' para médico inexistente")
    void medicoInexistente() {
        when(medicos.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.agendar(99L, PACIENTE_ID, TERCA_10H));
    }

    // ---------- T7 · cancelar ----------

    @Test
    @DisplayName("cancela com mais de 24h de antecedência")
    void cancelaComAntecedencia() {
        Consulta quinta = new Consulta(medico, paciente, LocalDateTime.of(2026, 10, 22, 10, 0), LocalDateTime.now(RELOGIO));
        when(consultas.findById(5L)).thenReturn(Optional.of(quinta));

        ConsultaResponse resposta = service.cancelar(5L);

        assertEquals("CANCELADA", resposta.status());
    }

    @Test
    @DisplayName("recusa cancelamento com menos de 24h e mantém a consulta AGENDADA")
    void recusaCancelamentoTardio() {
        Consulta amanhaCedo = new Consulta(medico, paciente, LocalDateTime.of(2026, 10, 20, 8, 0), LocalDateTime.now(RELOGIO));
        when(consultas.findById(5L)).thenReturn(Optional.of(amanhaCedo));

        assertEquals(Violacao.CANCELAMENTO_TARDIO, violacao(() -> service.cancelar(5L)));
        assertEquals(StatusConsulta.AGENDADA, amanhaCedo.getStatus());
    }

    @Test
    @DisplayName("recusa cancelar a mesma consulta duas vezes")
    void recusaCancelarDuasVezes() {
        Consulta quinta = new Consulta(medico, paciente, LocalDateTime.of(2026, 10, 22, 10, 0), LocalDateTime.now(RELOGIO));
        when(consultas.findById(5L)).thenReturn(Optional.of(quinta));
        service.cancelar(5L);

        assertEquals(Violacao.CONSULTA_JA_CANCELADA, violacao(() -> service.cancelar(5L)));
    }

    // ---------- T11 · agenda do dia ----------

    @Test
    @DisplayName("busca a agenda no intervalo [00:00 do dia, 00:00 do dia seguinte)")
    void agendaDoDiaUsaIntervaloDoDia() {
        Consulta c = new Consulta(medico, paciente, TERCA_10H, LocalDateTime.now(RELOGIO));
        when(medicos.existsById(MEDICO_ID)).thenReturn(true);
        when(consultas.agendaDoMedico(MEDICO_ID, StatusConsulta.AGENDADA,
                LocalDateTime.of(2026, 10, 20, 0, 0), LocalDateTime.of(2026, 10, 21, 0, 0)))
                .thenReturn(List.of(c));

        List<ConsultaResponse> agenda = service.agendaDoDia(MEDICO_ID, LocalDate.of(2026, 10, 20));

        assertEquals(1, agenda.size());
        assertEquals(TERCA_10H, agenda.get(0).inicio());
    }
}
