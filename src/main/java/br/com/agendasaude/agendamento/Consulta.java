package br.com.agendasaude.agendamento;

import br.com.agendasaude.cadastro.Medico;
import br.com.agendasaude.cadastro.Paciente;
import br.com.agendasaude.dominio.RegraDeNegocioException;
import br.com.agendasaude.dominio.Violacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "consulta")
public class Consulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusConsulta status;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    protected Consulta() {
        // exigido pelo JPA
    }

    public Consulta(Medico medico, Paciente paciente, LocalDateTime inicio, LocalDateTime criadaEm) {
        this.medico = medico;
        this.paciente = paciente;
        this.inicio = inicio;
        this.criadaEm = criadaEm;
        this.status = StatusConsulta.AGENDADA;
    }

    /** A checagem de antecedência é do serviço (depende do relógio); aqui só a transição de estado. */
    public void cancelar() {
        if (status == StatusConsulta.CANCELADA) {
            throw new RegraDeNegocioException(Violacao.CONSULTA_JA_CANCELADA);
        }
        this.status = StatusConsulta.CANCELADA;
    }

    public Long getId() { return id; }
    public Medico getMedico() { return medico; }
    public Paciente getPaciente() { return paciente; }
    public LocalDateTime getInicio() { return inicio; }
    public StatusConsulta getStatus() { return status; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
}
