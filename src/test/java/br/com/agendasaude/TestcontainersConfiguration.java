package br.com.agendasaude;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Sobe um PostgreSQL de verdade num container descartável para os testes *IT.
 * Mesma imagem do compose.yaml: o banco do teste é o banco de produção, não um H2 "parecido".
 * {@code @ServiceConnection} injeta URL, usuário e senha do container no datasource do Spring.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
    }
}
