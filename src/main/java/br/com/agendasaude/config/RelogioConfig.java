package br.com.agendasaude.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * O "agora" do sistema é um bean injetável. Em produção é o relógio real no fuso da clínica;
 * nos testes de aceitação é substituído por um relógio fixo — sem isso, regras como
 * "24 h de antecedência" dariam testes que passam ou falham conforme o horário em que rodam.
 */
@Configuration(proxyBeanMethods = false)
public class RelogioConfig {

    @Bean
    Clock relogio(@Value("${app.fuso}") String fuso) {
        return Clock.system(ZoneId.of(fuso));
    }
}
