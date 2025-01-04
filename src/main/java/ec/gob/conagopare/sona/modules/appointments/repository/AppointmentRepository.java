package ec.gob.conagopare.sona.modules.appointments.repository;

import ec.gob.conagopare.sona.modules.appointments.dto.AppoimentDetails;
import ec.gob.conagopare.sona.modules.appointments.models.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("""
            SELECT COUNT(a) > 0 FROM Appointment a
            WHERE a.professional.id = :professionalId
            AND a.date = :date
            AND a.hour = :hour
            AND a.canceled = false
            """)
    boolean existsAppointmentAtHour(
            @Param("professionalId") Long professionalId,
            @Param("date") LocalDate date,
            @Param("hour") Integer hour
    );

    @Query("""
            SELECT COUNT(ps) > 0 FROM ProfessionalSchedule ps
            WHERE ps.professional.id = :professionalId
            AND ps.date = :date
            AND :hour >= ps.fromHour
            AND :hour < ps.toHour
            """)
    boolean isWithinProfessionalSchedule(
            @Param("professionalId") Long professionalId,
            @Param("date") LocalDate date,
            @Param("hour") Integer hour
    );

    @Query("""
            SELECT new ec.gob.conagopare.sona.modules.appointments.dto.AppoimentDetails(a.date, a.hour, a.hour + 1, a.type) FROM Appointment a
            WHERE a.professional.id = :professionalId
            AND a.date BETWEEN :from AND :to
            AND a.canceled = false
            """)
    List<AppoimentDetails> getProfessionalAppointments(
            @Param("professionalId") Long professionalId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
