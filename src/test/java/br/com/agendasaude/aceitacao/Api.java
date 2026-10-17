package br.com.agendasaude.aceitacao;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente HTTP mínimo (JDK puro) para os testes de aceitação: o teste fala com a API como um
 * usuário falaria — por HTTP e JSON — sem atalhos para dentro do código.
 */
public class Api {

    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    private final String base;
    private final HttpClient http = HttpClient.newHttpClient();

    public Api(int porta) {
        this.base = "http://localhost:" + porta;
    }

    public record Resposta(int status, String corpo, Optional<String> location) {
        public Long id() {
            Matcher m = ID.matcher(corpo);
            if (!m.find()) {
                throw new AssertionError("Resposta sem campo id: " + corpo);
            }
            return Long.valueOf(m.group(1));
        }
    }

    public Resposta get(String caminho) {
        return enviar(HttpRequest.newBuilder(URI.create(base + caminho)).GET().build());
    }

    public Resposta post(String caminho, String json) {
        return enviar(HttpRequest.newBuilder(URI.create(base + caminho))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build());
    }

    public Resposta patch(String caminho) {
        return enviar(HttpRequest.newBuilder(URI.create(base + caminho))
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build());
    }

    // ---------- atalhos para os "Dado ..." dos cenários ----------

    public Long cadastrarMedico() {
        String crm = "CRM-AC " + UUID.randomUUID().toString().substring(0, 8);
        Resposta r = post("/api/medicos", """
                {"nome": "Dra. Aceitação", "especialidade": "Clínica geral", "crm": "%s"}
                """.formatted(crm));
        exigir(201, r);
        return r.id();
    }

    public Long cadastrarPaciente(String nome) {
        String email = nome.toLowerCase().replace(' ', '.') + "-" + UUID.randomUUID() + "@teste.com";
        Resposta r = post("/api/pacientes", """
                {"nome": "%s", "email": "%s"}
                """.formatted(nome, email));
        exigir(201, r);
        return r.id();
    }

    public Resposta agendar(Long medicoId, Long pacienteId, String inicio) {
        return post("/api/consultas", """
                {"medicoId": %d, "pacienteId": %d, "inicio": "%s"}
                """.formatted(medicoId, pacienteId, inicio));
    }

    private static void exigir(int status, Resposta r) {
        if (r.status() != status) {
            throw new AssertionError("Esperava HTTP " + status + " mas veio " + r.status() + ": " + r.corpo());
        }
    }

    private Resposta enviar(HttpRequest requisicao) {
        try {
            HttpResponse<String> resposta = http.send(requisicao, HttpResponse.BodyHandlers.ofString());
            return new Resposta(resposta.statusCode(), resposta.body(), resposta.headers().firstValue("Location"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
