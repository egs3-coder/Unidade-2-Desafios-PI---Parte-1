package br.com.agendasaude.agendamento;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/** Camada fina: traduz HTTP ↔ casos de uso. Nenhuma regra de negócio mora aqui. */
@RestController
@RequestMapping("/api")
public class ConsultaController {

    private final AgendamentoService service;

    public ConsultaController(AgendamentoService service) {
        this.service = service;
    }

    /** H1 · T5 */
    @PostMapping("/consultas")
    public ResponseEntity<ConsultaResponse> agendar(@Valid @RequestBody NovaConsultaRequest requisicao) {
        ConsultaResponse criada = service.agendar(requisicao.medicoId(), requisicao.pacienteId(), requisicao.inicio());
        return ResponseEntity.created(URI.create("/api/consultas/" + criada.id())).body(criada);
    }

    @GetMapping("/consultas/{id}")
    public ConsultaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    /** H2 · T9 */
    @PatchMapping("/consultas/{id}/cancelamento")
    public ConsultaResponse cancelar(@PathVariable Long id) {
        return service.cancelar(id);
    }

    /** H3 · T12 */
    @GetMapping("/medicos/{id}/agenda")
    public List<ConsultaResponse> agendaDoDia(@PathVariable Long id,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return service.agendaDoDia(id, data);
    }
}
