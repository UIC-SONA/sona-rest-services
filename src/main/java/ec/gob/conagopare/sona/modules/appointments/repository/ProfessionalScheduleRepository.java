package ec.gob.conagopare.sona.modules.appointments.repository;

import ec.gob.conagopare.sona.modules.appointments.models.ProfessionalSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface ProfessionalScheduleRepository extends JpaRepository<ProfessionalSchedule, Long> {

    @Query("""
            SELECT COUNT(ps) > 0 FROM ProfessionalSchedule ps
            WHERE ps.professional.id = :professionalId
            AND ps.date = :date
            AND (
                (:fromHour >= ps.fromHour AND :fromHour < ps.toHour) OR
                (:toHour > ps.fromHour AND :toHour <= ps.toHour) OR
                (:fromHour <= ps.fromHour AND :toHour >= ps.toHour)
            )
            """)
    boolean existsOverlappingSchedule(
            @Param("professionalId") Long professionalId,
            @Param("date") LocalDate date,
            @Param("fromHour") Integer fromHour,
            @Param("toHour") Integer toHour
    );

    @Query("""
            SELECT COUNT(ps) > 0 FROM ProfessionalSchedule ps
            WHERE ps.professional.id = :professionalId
            AND ps.date = :date
            AND ps.id != :scheduleId
            AND (
                (:fromHour >= ps.fromHour AND :fromHour < ps.toHour) OR
                (:toHour > ps.fromHour AND :toHour <= ps.toHour) OR
                (:fromHour <= ps.fromHour AND :toHour >= ps.toHour)
            )
            """)
    boolean existsOverlappingScheduleExcludingId(
            @Param("professionalId") Long professionalId,
            @Param("date") LocalDate date,
            @Param("fromHour") Integer fromHour,
            @Param("toHour") Integer toHour,
            @Param("scheduleId") Long scheduleId
    );

    @Query("""
            SELECT COUNT(a) > 0 FROM Appointment a
            WHERE a.professional.id = :professionalId
            AND a.date = :date
            AND a.hour >= :fromHour
            AND a.hour < :toHour
            AND a.canceled = false
            """)
    boolean existsActiveAppointmentsInSchedule(
            @Param("professionalId") Long professionalId,
            @Param("date") LocalDate date,
            @Param("fromHour") Integer fromHour,
            @Param("toHour") Integer toHour
    );

}
