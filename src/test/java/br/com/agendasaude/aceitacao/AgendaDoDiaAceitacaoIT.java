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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T12 · Aceitação da H3 "Ver a agenda do dia do médico". */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, RelogioDeTesteConfig.class})
@DisplayName("H3 · Agenda do dia (aceitação)")
class AgendaDoDiaAceitacaoIT {

    @Value("${local.server.port}")
    int porta;

    private Api api;

    @BeforeEach
    void cliente() {
        api = new Api(porta);
    }

    @Test
    @DisplayName("Cenário: agenda ordenada por horário e sem consultas canceladas")
    void agendaOrdenadaESemCanceladas() {
        // Dado que o médico tem consultas às 10:00 e às 08:30 e uma cancelada às 09:00 (quarta 21/10)
        Long medico = api.cadastrarMedico();
        api.agendar(medico, api.cadastrarPaciente("Paciente Das Dez"), "2026-10-21T10:00:00");
        api.agendar(medico, api.cadastrarPaciente("Paciente Das Oito"), "2026-10-21T08:30:00");
        Long cancelada = api.agendar(medico, api.cadastrarPaciente("Paciente Cancelado"), "2026-10-21T09:00:00").id();
        assertEquals(200, api.patch("/api/consultas/" + cancelada + "/cancelamento").status());

        // Quando a recepcionista consulta a agenda daquele dia
        Resposta r = api.get("/api/medicos/" + medico + "/agenda?data=2026-10-21");

        // Então vê 08:30 antes de 10:00 e não vê a consulta cancelada
        assertEquals(200, r.status(), r.corpo());
        int oitoEMeia = r.corpo().indexOf("T08:30");
        int dez = r.corpo().indexOf("T10:00");
        assertTrue(oitoEMeia >= 0 && dez > oitoEMeia, "ordem errada: " + r.corpo());
        assertFalse(r.corpo().contains("Paciente Cancelado"), r.corpo());
    }

    @Test
    @DisplayName("Cenário: dia sem consultas → lista vazia")
    void diaSemConsultas() {
        Long medico = api.cadastrarMedico();

        Resposta r = api.get("/api/medicos/" + medico + "/agenda?data=2026-10-23");

        assertEquals(200, r.status());
        assertEquals("[]", r.corpo());
    }
}
