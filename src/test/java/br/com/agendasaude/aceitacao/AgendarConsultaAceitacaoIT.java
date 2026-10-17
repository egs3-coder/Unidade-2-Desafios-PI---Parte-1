package br.com.agendasaude.aceitacao;

import br.com.agendasaude.TestcontainersConfiguration;
import br.com.agendasaude.aceitacao.Api.Resposta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T5 · Aceitação da H1 "Agendar consulta em horário livre".
 * Cada método é um cenário Gherkin de docs/story-map.md (Dado / Quando / Então).
 * Aplicação inteira de pé (porta aleatória) + PostgreSQL real + relógio congelado em segunda 09:00.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, RelogioDeTesteConfig.class})
@DisplayName("H1 · Agendar consulta (aceitação)")
class AgendarConsultaAceitacaoIT {

    @Value("${local.server.port}")
    int porta;

    private Api api;

    @BeforeEach
    void cliente() {
        api = new Api(porta);
    }

    @Test
    @DisplayName("Cenário: horário livre dentro do expediente → 201 AGENDADA")
    void horarioLivre() {
        // Dado um médico e um paciente cadastrados
        Long medico = api.cadastrarMedico();
        Long paciente = api.cadastrarPaciente("Paciente H1");

        // Quando o paciente agenda terça-feira às 09:00
        Resposta r = api.agendar(medico, paciente, "2026-10-20T09:00:00");

        // Então a consulta é criada com status AGENDADA
        assertEquals(201, r.status(), r.corpo());
        assertTrue(r.corpo().contains("\"status\":\"AGENDADA\""), r.corpo());
        assertTrue(r.location().orElseThrow().endsWith("/api/consultas/" + r.id()));
    }

    @Test
    @DisplayName("Cenário: horário já ocupado pelo médico → 409 MEDICO_OCUPADO")
    void horarioOcupado() {
        // Dado que o médico já tem consulta terça-feira às 10:00
        Long medico = api.cadastrarMedico();
        assertEquals(201, api.agendar(medico, api.cadastrarPaciente("Primeiro"), "2026-10-20T10:00:00").status());

        // Quando outro paciente tenta agendar o mesmo horário
        Resposta r = api.agendar(medico, api.cadastrarPaciente("Segundo"), "2026-10-20T10:00:00");

        // Então o agendamento é recusado
        assertEquals(409, r.status(), r.corpo());
        assertTrue(r.corpo().contains("MEDICO_OCUPADO"), r.corpo());
    }

    @Test
    @DisplayName("Cenário: fora do expediente (sábado) → 422 FORA_DO_EXPEDIENTE")
    void foraDoExpediente() {
        Long medico = api.cadastrarMedico();
        Long paciente = api.cadastrarPaciente("Paciente Sabado");

        Resposta r = api.agendar(medico, paciente, "2026-10-24T10:00:00");

        assertEquals(422, r.status(), r.corpo());
        assertTrue(r.corpo().contains("FORA_DO_EXPEDIENTE"), r.corpo());
    }

    @Test
    @DisplayName("Cenário: médico inexistente → 404")
    void medicoInexistente() {
        Resposta r = api.agendar(999_999L, api.cadastrarPaciente("Paciente 404"), "2026-10-20T11:00:00");

        assertEquals(404, r.status(), r.corpo());
    }

    @Test
    @DisplayName("Cenário: requisição sem horário → 400 com o campo inválido")
    void requisicaoInvalida() {
        Resposta r = api.post("/api/consultas", "{\"medicoId\": 1, \"pacienteId\": 1}");

        assertEquals(400, r.status(), r.corpo());
        assertTrue(r.corpo().contains("inicio"), r.corpo());
    }
}
