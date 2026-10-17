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

/** T9 · Aceitação da H2 "Cancelar consulta". Agora = segunda 19/10/2026 09:00. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, RelogioDeTesteConfig.class})
@DisplayName("H2 · Cancelar consulta (aceitação)")
class CancelarConsultaAceitacaoIT {

    @Value("${local.server.port}")
    int porta;

    private Api api;

    @BeforeEach
    void cliente() {
        api = new Api(porta);
    }

    @Test
    @DisplayName("Cenário: cancelamento com antecedência → 200 CANCELADA e o horário volta a ficar livre")
    void cancelamentoComAntecedencia() {
        // Dado uma consulta agendada para daqui a 3 dias (quinta 10:00)
        Long medico = api.cadastrarMedico();
        Long consulta = api.agendar(medico, api.cadastrarPaciente("Cancela Cedo"), "2026-10-22T10:00:00").id();

        // Quando o paciente cancela
        Resposta r = api.patch("/api/consultas/" + consulta + "/cancelamento");

        // Então a consulta fica CANCELADA
        assertEquals(200, r.status(), r.corpo());
        assertTrue(r.corpo().contains("\"status\":\"CANCELADA\""), r.corpo());
        // E o horário volta a ficar disponível para outro paciente
        assertEquals(201, api.agendar(medico, api.cadastrarPaciente("Novo Paciente"), "2026-10-22T10:00:00").status());
    }

    @Test
    @DisplayName("Cenário: cancelamento em cima da hora → 422 CANCELAMENTO_TARDIO e a consulta continua AGENDADA")
    void cancelamentoTardio() {
        // Dado uma consulta agendada para daqui a 23 horas (terça 08:00)
        Long medico = api.cadastrarMedico();
        Long consulta = api.agendar(medico, api.cadastrarPaciente("Cancela Tarde"), "2026-10-20T08:00:00").id();

        // Quando o paciente tenta cancelar
        Resposta r = api.patch("/api/consultas/" + consulta + "/cancelamento");

        // Então o cancelamento é recusado
        assertEquals(422, r.status(), r.corpo());
        assertTrue(r.corpo().contains("CANCELAMENTO_TARDIO"), r.corpo());
        assertTrue(api.get("/api/consultas/" + consulta).corpo().contains("\"status\":\"AGENDADA\""));
    }
}
