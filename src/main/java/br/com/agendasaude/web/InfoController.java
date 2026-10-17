package br.com.agendasaude.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Página inicial da API e versão publicada. O smoke test da esteira chama /api/versao e só considera
 * o deploy concluído quando a versão no ar é o SHA do commit que acabou de passar pela esteira.
 */
@RestController
public class InfoController {

    private final String versao;

    public InfoController(@Value("${app.versao}") String versao) {
        this.versao = versao;
    }

    @GetMapping("/api/versao")
    public Map<String, String> versao() {
        return Map.of("versao", versao);
    }

    @GetMapping("/")
    public Map<String, Object> inicio() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("aplicacao", "AgendaSaúde — agendamento de consultas");
        info.put("versao", versao);
        info.put("endpoints", List.of(
                "GET   /api/medicos",
                "POST  /api/medicos",
                "GET   /api/pacientes",
                "POST  /api/pacientes",
                "POST  /api/consultas",
                "GET   /api/consultas/{id}",
                "PATCH /api/consultas/{id}/cancelamento",
                "GET   /api/medicos/{id}/agenda?data=AAAA-MM-DD",
                "GET   /api/versao",
                "GET   /actuator/health"));
        return info;
    }
}
