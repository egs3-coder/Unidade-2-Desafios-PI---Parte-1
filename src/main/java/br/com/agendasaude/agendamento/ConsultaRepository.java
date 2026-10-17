package br.com.agendasaude.agendamento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    boolean existsByMedicoIdAndInicioAndStatus(Long medicoId, LocalDateTime inicio, StatusConsulta status);

    boolean existsByPacienteIdAndInicioAndStatus(Long pacienteId, LocalDateTime inicio, StatusConsulta status);

    /** Consultas do médico em [de, ate), em ordem de horário — já trazendo médico e paciente (evita N+1). */
    @Query("""
            select c from Consulta c
              join fetch c.medico m
              join fetch c.paciente p
             where m.id = :medicoId
               and c.status = :status
               and c.inicio >= :de and c.inicio < :ate
             order by c.inicio
            """)
    List<Consulta> agendaDoMedico(@Param("medicoId") Long medicoId,
                                  @Param("status") StatusConsulta status,
                                  @Param("de") LocalDateTime de,
                                  @Param("ate") LocalDateTime ate);
}
