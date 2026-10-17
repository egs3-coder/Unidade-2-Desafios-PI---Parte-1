package br.com.agendasaude.dominio;

import java.time.Duration;
import java.time.LocalDateTime;

/** T6 · Cancelamento exige antecedência mínima de 24 horas (borda inclusiva). */
public class PoliticaDeCancelamento {

    public static final Duration ANTECEDENCIA_MINIMA = Duration.ofHours(24);

    public void validarCancelamento(LocalDateTime inicioDaConsulta, LocalDateTime agora) {
        Duration antecedencia = Duration.between(agora, inicioDaConsulta);
        if (antecedencia.compareTo(ANTECEDENCIA_MINIMA) < 0) {
            throw new RegraDeNegocioException(Violacao.CANCELAMENTO_TARDIO);
        }
    }
}
