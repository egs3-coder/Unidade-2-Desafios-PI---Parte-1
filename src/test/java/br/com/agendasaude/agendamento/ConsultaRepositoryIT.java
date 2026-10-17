package br.com.agendasaude.agendamento;

import br.com.agendasaude.TestcontainersConfiguration;
import br.com.agendasaude.cadastro.Medico;
import br.com.agendasaude.cadastro.MedicoRepository;
import br.com.agendasaude.cadastro.Paciente;
import br.com.agendasaude.cadastro.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T4, T8 e T10 · Integração: código + PostgreSQL real (Testcontainers).
 * O que só o banco garante: migrações, índices únicos e a consulta JPQL da agenda.
 * {@code @Transactional}: cada teste é desfeito no fim (rollback), então um não suja o outro.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
@DisplayName("T4/T8/T10 · ConsultaRepository com PostgreSQL real")
class ConsultaRepositoryIT {

    private static final LocalDateTime TERCA_10H = LocalDateTime.of(2026, 10, 20, 10, 0);
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 19, 9, 0);

    @Autowired ConsultaRepository consultas;
    @Autowired MedicoRepository medicos;
    @Autowired PacienteRepository pacientes;

    private Medico medico;
    private Paciente ana;
    private Paciente bruno;

    @BeforeEach
    void cadastros() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        medico = medicos.save(new Medico("Dr. Integração", "Cardiologia", "CRM-IT " + sufixo));
        ana = pacientes.save(new Paciente("Ana", "ana-" + sufixo + "@teste.com"));
        bruno = pacientes.save(new Paciente("Bruno", "bruno-" + sufixo + "@teste.com"));
    }

    private Consulta consulta(Paciente paciente, LocalDateTime inicio) {
        return new Consulta(medico, paciente, inicio, AGORA);
    }

    @Test
    @DisplayName("as migrações do Flyway criam o schema e os dados iniciais")
    void migracoesAplicadas() {
        assertTrue(medicos.count() >= 3, "V2__dados_iniciais deveria cadastrar 3 médicos");
    }

    @Test
    @DisplayName("T4 · grava e encontra consulta AGENDADA pelo médico e horário")
    void gravaELocalizaPorHorario() {
        consultas.saveAndFlush(consulta(ana, TERCA_10H));

        assertTrue(consultas.existsByMedicoIdAndInicioAndStatus(medico.getId(), TERCA_10H, StatusConsulta.AGENDADA));
        assertTrue(consultas.existsByPacienteIdAndInicioAndStatus(ana.getId(), TERCA_10H, StatusConsulta.AGENDADA));
        assertFalse(consultas.existsByPacienteIdAndInicioAndStatus(bruno.getId(), TERCA_10H, StatusConsulta.AGENDADA));
    }

    @Test
    @DisplayName("T4 · o índice único do banco barra dois pacientes no mesmo horário do médico (corrida entre requisições)")
    void indiceUnicoBarraConflitoDoMedico() {
        consultas.saveAndFlush(consulta(ana, TERCA_10H));

        assertThrows(DataIntegrityViolationException.class,
                () -> consultas.saveAndFlush(consulta(bruno, TERCA_10H)));
    }

    @Test
    @DisplayName("T8 · consulta CANCELADA libera o horário (o índice é parcial: WHERE status = 'AGENDADA')")
    void canceladaLiberaHorario() {
        Consulta cancelada = consulta(ana, TERCA_10H);
        cancelada.cancelar();
        consultas.saveAndFlush(cancelada);

        assertDoesNotThrow(() -> consultas.saveAndFlush(consulta(bruno, TERCA_10H)));
    }

    @Test
    @DisplayName("T10 · agenda do dia vem em ordem de horário, sem canceladas e sem outros dias")
    void agendaDoDiaOrdenadaESemCanceladas() {
        Paciente carla = pacientes.save(new Paciente("Carla", "carla-" + UUID.randomUUID() + "@teste.com"));
        consultas.save(consulta(ana, TERCA_10H));
        consultas.save(consulta(bruno, TERCA_10H.withHour(8).withMinute(30)));
        Consulta cancelada = consulta(carla, TERCA_10H.withHour(9));
        cancelada.cancelar();
        consultas.save(cancelada);
        consultas.save(consulta(carla, TERCA_10H.plusDays(1)));
        consultas.flush();

        List<Consulta> agenda = consultas.agendaDoMedico(medico.getId(), StatusConsulta.AGENDADA,
                TERCA_10H.toLocalDate().atStartOfDay(), TERCA_10H.toLocalDate().plusDays(1).atStartOfDay());

        assertEquals(List.of("Bruno", "Ana"), agenda.stream().map(c -> c.getPaciente().getNome()).toList());
    }
}
