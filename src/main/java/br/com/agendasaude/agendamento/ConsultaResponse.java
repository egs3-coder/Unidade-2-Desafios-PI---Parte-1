package br.com.agendasaude.agendamento;

import java.time.LocalDateTime;

/** O que a API devolve sobre uma consulta (a entidade JPA nunca sai da camada de serviço). */
public record ConsultaResponse(
        Long id,
        Long medicoId,
        String medico,
        Long pacienteId,
        String paciente,
        LocalDateTime inicio,
        String status) {

    public static ConsultaResponse de(Consulta c) {
        return new ConsultaResponse(
                c.getId(),
                c.getMedico().getId(),
                c.getMedico().getNome(),
                c.getPaciente().getId(),
                c.getPaciente().getNome(),
                c.getInicio(),
                c.getStatus().name());
    }
}
