package br.com.agendasaude.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** T6 · Cancelamento exige antecedência mínima de 24 horas. */
@DisplayName("T6 · Política de cancelamento")
class PoliticaDeCancelamentoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 19, 9, 0);

    private final PoliticaDeCancelamento politica = new PoliticaDeCancelamento();

    private Violacao violacaoAoCancelar(LocalDateTime inicioDaConsulta) {
        return assertThrows(RegraDeNegocioException.class,
                () -> politica.validarCancelamento(inicioDaConsulta, AGORA)).getViolacao();
    }

    @Test
    @DisplayName("permite cancelar com 48 horas de antecedência")
    void permiteComFolga() {
        assertDoesNotThrow(() -> politica.validarCancelamento(AGORA.plusHours(48), AGORA));
    }

    @Test
    @DisplayName("permite cancelar com exatamente 24 horas (borda inclusiva)")
    void permiteNoLimite() {
        assertDoesNotThrow(() -> politica.validarCancelamento(AGORA.plusHours(24), AGORA));
    }

    @Test
    @DisplayName("recusa com 23h59 de antecedência")
    void recusaUmMinutoAntesDoLimite() {
        assertEquals(Violacao.CANCELAMENTO_TARDIO, violacaoAoCancelar(AGORA.plusHours(23).plusMinutes(59)));
    }

    @Test
    @DisplayName("recusa consulta que já aconteceu")
    void recusaConsultaPassada() {
        assertEquals(Violacao.CANCELAMENTO_TARDIO, violacaoAoCancelar(AGORA.minusDays(1)));
    }
}
