package ec.gob.conagopare.sona.modules.user.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

@Data
public class ProfessionalScheduleDto {

    @NotNull
    private Long professionalId;

    @NotNull
    @Future
    private LocalDate scheduleUpTo;

    @NotNull
    @NotEmpty
    private Set<DayOfWeek> scheduleDays;

    @NotNull
    @Positive
    @Range(min = 0, max = 23)
    private Integer scheduleStartHour;

    @NotNull
    @Positive
    @Range(min = 0, max = 23)
    private Integer scheduleEndHour;

}