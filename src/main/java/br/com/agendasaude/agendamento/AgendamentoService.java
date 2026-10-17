package br.com.agendasaude.agendamento;

import br.com.agendasaude.cadastro.Medico;
import br.com.agendasaude.cadastro.MedicoRepository;
import br.com.agendasaude.cadastro.Paciente;
import br.com.agendasaude.cadastro.PacienteRepository;
import br.com.agendasaude.dominio.Expediente;
import br.com.agendasaude.dominio.JanelaDeAgendamento;
import br.com.agendasaude.dominio.PoliticaDeCancelamento;
import br.com.agendasaude.dominio.RecursoNaoEncontradoException;
import br.com.agendasaude.dominio.RegraDeNegocioException;
import br.com.agendasaude.dominio.Violacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Casos de uso da agenda. As regras puras ficam no pacote {@code dominio}; aqui elas são orquestradas
 * com o estado do banco. Ordem proposital: primeiro as regras baratas (sem banco), depois as consultas.
 */
@Service
public class AgendamentoService {

    private final ConsultaRepository consultas;
    private final MedicoRepository medicos;
    private final PacienteRepository pacientes;
    private final Clock relogio;

    private final Expediente expediente = new Expediente();
    private final JanelaDeAgendamento janela = new JanelaDeAgendamento();
    private final PoliticaDeCancelamento politicaDeCancelamento = new PoliticaDeCancelamento();

    public AgendamentoService(ConsultaRepository consultas, MedicoRepository medicos,
                              PacienteRepository pacientes, Clock relogio) {
        this.consultas = consultas;
        this.medicos = medicos;
        this.pacientes = pacientes;
        this.relogio = relogio;
    }

    /** T3 · Agenda se o horário é válido e está livre para o médico e para o paciente. */
    @Transactional
    public ConsultaResponse agendar(Long medicoId, Long pacienteId, LocalDateTime inicio) {
        expediente.validar(inicio);
        janela.validar(inicio, agora());

        Medico medico = medicos.findById(medicoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Médico", medicoId));
        Paciente paciente = pacientes.findById(pacienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paciente", pacienteId));

        if (consultas.existsByMedicoIdAndInicioAndStatus(medicoId, inicio, StatusConsulta.AGENDADA)) {
            throw new RegraDeNegocioException(Violacao.MEDICO_OCUPADO);
        }
        if (consultas.existsByPacienteIdAndInicioAndStatus(pacienteId, inicio, StatusConsulta.AGENDADA)) {
            throw new RegraDeNegocioException(Violacao.PACIENTE_OCUPADO);
        }
        // Duas requisições simultâneas podem passar pelas checagens acima; o índice único parcial do
        // banco (V1__estrutura.sql) é a última barreira e vira HTTP 409 no ApiExceptionHandler.
        Consulta salva = consultas.save(new Consulta(medico, paciente, inicio, agora()));
        return ConsultaResponse.de(salva);
    }

    /** T7 · Cancela respeitando a antecedência mínima; não cancela duas vezes. */
    @Transactional
    public ConsultaResponse cancelar(Long consultaId) {
        Consulta consulta = buscarEntidade(consultaId);
        if (consulta.getStatus() == StatusConsulta.CANCELADA) {
            throw new RegraDeNegocioException(Violacao.CONSULTA_JA_CANCELADA);
        }
        politicaDeCancelamento.validarCancelamento(consulta.getInicio(), agora());
        consulta.cancelar();
        return ConsultaResponse.de(consulta);
    }

    /** T11 · Consultas AGENDADAS do médico no dia, em ordem de horário. */
    @Transactional(readOnly = true)
    public List<ConsultaResponse> agendaDoDia(Long medicoId, LocalDate dia) {
        if (!medicos.existsById(medicoId)) {
            throw new RecursoNaoEncontradoException("Médico", medicoId);
        }
        return consultas.agendaDoMedico(medicoId, StatusConsulta.AGENDADA,
                        dia.atStartOfDay(), dia.plusDays(1).atStartOfDay())
                .stream()
                .map(ConsultaResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConsultaResponse buscar(Long consultaId) {
        return ConsultaResponse.de(buscarEntidade(consultaId));
    }

    private Consulta buscarEntidade(Long consultaId) {
        return consultas.findById(consultaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Consulta", consultaId));
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }
}
