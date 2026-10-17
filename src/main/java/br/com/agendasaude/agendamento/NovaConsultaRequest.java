package br.com.agendasaude.agendamento;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** Corpo do POST /api/consultas. Ex.: {"medicoId": 1, "pacienteId": 2, "inicio": "2026-10-20T09:00:00"} */
public record NovaConsultaRequest(
        @NotNull Long medicoId,
        @NotNull Long pacienteId,
        @NotNull LocalDateTime inicio) {
}
