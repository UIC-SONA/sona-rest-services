package ec.gob.conagopare.sona.modules.appointments.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;


@EqualsAndHashCode(callSuper = true)
@Data
public class ProfessionalScheduleDto extends ScheduleBaseDto {
    //
    @NotNull
    @FutureOrPresent
    private LocalDate date;
}
