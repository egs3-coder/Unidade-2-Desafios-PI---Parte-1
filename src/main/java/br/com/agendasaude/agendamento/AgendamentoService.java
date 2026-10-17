package br.com.agendasaude.agendamento;

import br.com.agendasaude.cadastro.MedicoRepository;
import br.com.agendasaude.cadastro.PacienteRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** T3/T7/T11 · Esqueleto gerado pela IDE a partir do teste: falha de propósito. */
@Service
public class AgendamentoService {

    public AgendamentoService(ConsultaRepository consultas, MedicoRepository medicos,
                              PacienteRepository pacientes, Clock relogio) {
    }

    public ConsultaResponse agendar(Long medicoId, Long pacienteId, LocalDateTime inicio) {
        throw new UnsupportedOperationException("não implementado");
    }

    public ConsultaResponse cancelar(Long consultaId) {
        throw new UnsupportedOperationException("não implementado");
    }

    public List<ConsultaResponse> agendaDoDia(Long medicoId, LocalDate dia) {
        throw new UnsupportedOperationException("não implementado");
    }
}
