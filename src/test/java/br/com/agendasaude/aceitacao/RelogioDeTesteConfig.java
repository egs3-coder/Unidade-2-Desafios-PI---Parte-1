package br.com.agendasaude.aceitacao;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Congela o "agora" da aplicação: os cenários de aceitação não dependem do dia em que a esteira roda. */
@TestConfiguration(proxyBeanMethods = false)
public class RelogioDeTesteConfig {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    /** Segunda-feira, 19/10/2026, 09:00. */
    public static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 19, 9, 0);

    @Bean
    @Primary
    Clock relogioDeTeste() {
        return Clock.fixed(AGORA.atZone(FUSO).toInstant(), FUSO);
    }
}
