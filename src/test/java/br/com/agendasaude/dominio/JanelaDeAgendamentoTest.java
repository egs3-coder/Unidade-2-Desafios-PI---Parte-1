package br.com.agendasaude.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** T2 · Só se agenda no futuro e até 60 dias à frente. "Agora" é parâmetro: o teste controla o relógio. */
@DisplayName("T2 · Janela de agendamento")
class JanelaDeAgendamentoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 19, 9, 0); // segunda, 09:00

    private final JanelaDeAgendamento janela = new JanelaDeAgendamento();

    private Violacao violacaoAo(LocalDateTime inicio) {
        return assertThrows(RegraDeNegocioException.class, () -> janela.validar(inicio, AGORA)).getViolacao();
    }

    @Test
    @DisplayName("aceita amanhã")
    void aceitaAmanha() {
        assertDoesNotThrow(() -> janela.validar(AGORA.plusDays(1), AGORA));
    }

    @Test
    @DisplayName("recusa ontem")
    void recusaPassado() {
        assertEquals(Violacao.HORARIO_NO_PASSADO, violacaoAo(AGORA.minusDays(1)));
    }

    @Test
    @DisplayName("recusa exatamente agora — a consulta já estaria começando")
    void recusaAgora() {
        assertEquals(Violacao.HORARIO_NO_PASSADO, violacaoAo(AGORA));
    }

    @Test
    @DisplayName("aceita exatamente 60 dias à frente (borda inclusiva)")
    void aceitaLimiteDaJanela() {
        assertDoesNotThrow(() -> janela.validar(AGORA.plusDays(60), AGORA));
    }

    @Test
    @DisplayName("recusa 60 dias e 30 minutos à frente")
    void recusaAlemDaJanela() {
        assertEquals(Violacao.ALEM_DA_JANELA, violacaoAo(AGORA.plusDays(60).plusMinutes(30)));
    }
}
