package br.com.agendasaude.cadastro;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Cadastros de apoio (Release 1: "cadastrar médico", "cadastrar paciente", "listar médicos").
 * CRUD sem regra de negócio: coberto indiretamente pelos testes de aceitação, sem teste unitário próprio.
 */
@RestController
@RequestMapping("/api")
public class CadastroController {

    public record NovoMedico(@NotBlank @Size(max = 120) String nome,
                             @NotBlank @Size(max = 80) String especialidade,
                             @NotBlank @Size(max = 20) String crm) {
    }

    public record MedicoResponse(Long id, String nome, String especialidade, String crm) {
        static MedicoResponse de(Medico m) {
            return new MedicoResponse(m.getId(), m.getNome(), m.getEspecialidade(), m.getCrm());
        }
    }

    public record NovoPaciente(@NotBlank @Size(max = 120) String nome,
                               @NotBlank @Email @Size(max = 160) String email) {
    }

    public record PacienteResponse(Long id, String nome, String email) {
        static PacienteResponse de(Paciente p) {
            return new PacienteResponse(p.getId(), p.getNome(), p.getEmail());
        }
    }

    private final MedicoRepository medicos;
    private final PacienteRepository pacientes;

    public CadastroController(MedicoRepository medicos, PacienteRepository pacientes) {
        this.medicos = medicos;
        this.pacientes = pacientes;
    }

    @GetMapping("/medicos")
    public List<MedicoResponse> listarMedicos() {
        return medicos.findAll().stream().map(MedicoResponse::de).toList();
    }

    @PostMapping("/medicos")
    public ResponseEntity<MedicoResponse> cadastrarMedico(@Valid @RequestBody NovoMedico novo) {
        Medico salvo = medicos.save(new Medico(novo.nome(), novo.especialidade(), novo.crm()));
        return ResponseEntity.created(URI.create("/api/medicos/" + salvo.getId())).body(MedicoResponse.de(salvo));
    }

    @GetMapping("/pacientes")
    public List<PacienteResponse> listarPacientes() {
        return pacientes.findAll().stream().map(PacienteResponse::de).toList();
    }

    @PostMapping("/pacientes")
    public ResponseEntity<PacienteResponse> cadastrarPaciente(@Valid @RequestBody NovoPaciente novo) {
        Paciente salvo = pacientes.save(new Paciente(novo.nome(), novo.email()));
        return ResponseEntity.created(URI.create("/api/pacientes/" + salvo.getId())).body(PacienteResponse.de(salvo));
    }
}
