package br.com.agendasaude.dominio;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** T1 · Regras de expediente da clínica. */
public class Expediente {

    public static final Duration DURACAO_DA_CONSULTA = Duration.ofMinutes(30);

    private static final LocalTime ABERTURA = LocalTime.of(8, 0);
    private static final LocalTime FECHAMENTO = LocalTime.of(18, 0);
    private static final LocalTime INICIO_DO_ALMOCO = LocalTime.of(12, 0);
    private static final LocalTime FIM_DO_ALMOCO = LocalTime.of(13, 0);

    public void validar(LocalDateTime inicio) {
        if (inicio.getMinute() % 30 != 0 || inicio.getSecond() != 0 || inicio.getNano() != 0) {
            throw new RegraDeNegocioException(Violacao.HORARIO_NAO_ALINHADO);
        }
        DayOfWeek dia = inicio.getDayOfWeek();
        if (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) {
            throw new RegraDeNegocioException(Violacao.FORA_DO_EXPEDIENTE);
        }
        LocalTime comeca = inicio.toLocalTime();
        LocalTime termina = comeca.plus(DURACAO_DA_CONSULTA);
        boolean dentroDoExpediente = !comeca.isBefore(ABERTURA) && !termina.isAfter(FECHAMENTO);
        boolean invadeAlmoco = comeca.isBefore(FIM_DO_ALMOCO) && termina.isAfter(INICIO_DO_ALMOCO);
        if (!dentroDoExpediente || invadeAlmoco) {
            throw new RegraDeNegocioException(Violacao.FORA_DO_EXPEDIENTE);
        }
    }
}
