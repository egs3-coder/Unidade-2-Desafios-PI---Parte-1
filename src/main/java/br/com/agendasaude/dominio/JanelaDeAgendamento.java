package br.com.agendasaude.dominio;

import java.time.LocalDateTime;

/** T2 · Só se agenda no futuro e no máximo {@value #DIAS_DE_AGENDA_ABERTA} dias à frente. */
public class JanelaDeAgendamento {

    public static final int DIAS_DE_AGENDA_ABERTA = 60;

    public void validar(LocalDateTime inicio, LocalDateTime agora) {
        if (!inicio.isAfter(agora)) {
            throw new RegraDeNegocioException(Violacao.HORARIO_NO_PASSADO);
        }
        if (inicio.isAfter(agora.plusDays(DIAS_DE_AGENDA_ABERTA))) {
            throw new RegraDeNegocioException(Violacao.ALEM_DA_JANELA);
        }
    }
}
