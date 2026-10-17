package br.com.agendasaude.dominio;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

/**
 * T1 · Regras de expediente da clínica.
 *
 * <p>Consultas de 30 minutos, começando em :00 ou :30, de segunda a sexta, entre 08:00 e 17:30
 * (último início), exceto 12:00 e 12:30 (almoço).
 *
 * <p>Lição do TDD: comparamos só o horário de <b>início</b> com limites fixos. Calcular o fim com
 * {@code LocalTime.plus} "dava a volta" na meia-noite (23:30 + 30 min = 00:00) e aceitava a consulta.
 */
public class Expediente {

    public static final Duration DURACAO_DA_CONSULTA = Duration.ofMinutes(30);

    private static final LocalTime PRIMEIRO_INICIO = LocalTime.of(8, 0);
    private static final LocalTime ULTIMO_INICIO = LocalTime.of(18, 0).minus(DURACAO_DA_CONSULTA);
    private static final LocalTime INICIO_DO_ALMOCO = LocalTime.of(12, 0);
    private static final LocalTime FIM_DO_ALMOCO = LocalTime.of(13, 0);
    private static final Set<DayOfWeek> FIM_DE_SEMANA = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    public void validar(LocalDateTime inicio) {
        if (!estaNaGrade(inicio)) {
            throw new RegraDeNegocioException(Violacao.HORARIO_NAO_ALINHADO);
        }
        if (FIM_DE_SEMANA.contains(inicio.getDayOfWeek()) || !estaNoHorarioDeAtendimento(inicio.toLocalTime())) {
            throw new RegraDeNegocioException(Violacao.FORA_DO_EXPEDIENTE);
        }
    }

    private static boolean estaNaGrade(LocalDateTime inicio) {
        return inicio.getMinute() % 30 == 0 && inicio.getSecond() == 0 && inicio.getNano() == 0;
    }

    private static boolean estaNoHorarioDeAtendimento(LocalTime comeca) {
        boolean dentroDoExpediente = !comeca.isBefore(PRIMEIRO_INICIO) && !comeca.isAfter(ULTIMO_INICIO);
        boolean noAlmoco = !comeca.isBefore(INICIO_DO_ALMOCO) && comeca.isBefore(FIM_DO_ALMOCO);
        return dentroDoExpediente && !noAlmoco;
    }
}
