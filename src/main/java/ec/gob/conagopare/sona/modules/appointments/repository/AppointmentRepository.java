package ec.gob.conagopare.sona.modules.appointments.repository;

import ec.gob.conagopare.sona.modules.appointments.dto.AppointmentRange;
import ec.gob.conagopare.sona.modules.appointments.models.Appointment;
import ec.gob.conagopare.sona.modules.user.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

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
            SELECT new ec.gob.conagopare.sona.modules.appointments.dto.AppointmentRange(a.date, a.hour, a.hour + 1) FROM Appointment a
            WHERE a.professional.id = :professionalId
            AND a.date BETWEEN :from AND :to
            AND a.canceled = false
            """)
    List<AppointmentRange> getProfessionalAppointmentsRanges(
            @Param("professionalId") Long professionalId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    Optional<Appointment> findByIdAndAttendantOrProfessional(Long id, User attendant, User professional);

    List<Appointment> findAllByIdInAndAttendantOrProfessional(List<Long> ids, User attendant, User professional);
}
