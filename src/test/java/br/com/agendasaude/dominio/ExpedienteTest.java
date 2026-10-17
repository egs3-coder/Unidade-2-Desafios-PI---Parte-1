package br.com.agendasaude.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * T1 · Expediente da clínica.
 * Lista de testes (BECK, 2003): do caso mais simples às bordas.
 * Datas de referência: 20/10/2026 é terça-feira; 24/10/2026 é sábado.
 */
@DisplayName("T1 · Expediente")
class ExpedienteTest {

    private final Expediente expediente = new Expediente();

    private static LocalDateTime terca(int hora, int minuto) {
        return LocalDateTime.of(2026, 10, 20, hora, minuto);
    }

    private Violacao violacaoAo(LocalDateTime inicio) {
        return assertThrows(RegraDeNegocioException.class, () -> expediente.validar(inicio)).getViolacao();
    }

    @Test
    @DisplayName("aceita terça às 09:00 (caso mais simples)")
    void aceitaHorarioComum() {
        assertDoesNotThrow(() -> expediente.validar(terca(9, 0)));
    }

    @Test
    @DisplayName("aceita 08:00, o primeiro horário do dia")
    void aceitaPrimeiroHorario() {
        assertDoesNotThrow(() -> expediente.validar(terca(8, 0)));
    }

    @Test
    @DisplayName("aceita 17:30, o último horário (termina às 18:00)")
    void aceitaUltimoHorario() {
        assertDoesNotThrow(() -> expediente.validar(terca(17, 30)));
    }

    @Test
    @DisplayName("recusa 07:30, antes da abertura")
    void recusaAntesDaAbertura() {
        assertEquals(Violacao.FORA_DO_EXPEDIENTE, violacaoAo(terca(7, 30)));
    }

    @Test
    @DisplayName("recusa 18:00, terminaria depois do fechamento")
    void recusaNoFechamento() {
        assertEquals(Violacao.FORA_DO_EXPEDIENTE, violacaoAo(terca(18, 0)));
    }

    @Test
    @DisplayName("recusa 12:00 e 12:30, horário de almoço")
    void recusaAlmoco() {
        assertEquals(Violacao.FORA_DO_EXPEDIENTE, violacaoAo(terca(12, 0)));
        assertEquals(Violacao.FORA_DO_EXPEDIENTE, violacaoAo(terca(12, 30)));
    }

    @Test
    @DisplayName("aceita 11:30 e 13:00, as bordas do almoço")
    void aceitaBordasDoAlmoco() {
        assertDoesNotThrow(() -> expediente.validar(terca(11, 30)));
        assertDoesNotThrow(() -> expediente.validar(terca(13, 0)));
    }

    @Test
    @DisplayName("recusa sábado e domingo")
    void recusaFimDeSemana() {
        assertEquals(Violacao.FORA_DO_EXPEDIENTE, violacaoAo(LocalDateTime.of(2026, 10, 24, 10, 0)));
        assertEquals(Violacao.FORA_DO_EXPEDIENTE, violacaoAo(LocalDateTime.of(2026, 10, 25, 10, 0)));
    }

    @Test
    @DisplayName("recusa 09:15, fora da grade de 30 minutos")
    void recusaHorarioQuebrado() {
        assertEquals(Violacao.HORARIO_NAO_ALINHADO, violacaoAo(terca(9, 15)));
    }

    @Test
    @DisplayName("recusa 09:00:30, segundos fora da grade")
    void recusaSegundos() {
        assertEquals(Violacao.HORARIO_NAO_ALINHADO, violacaoAo(terca(9, 0).withSecond(30)));
    }
}
