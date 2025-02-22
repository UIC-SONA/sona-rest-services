package ec.gob.conagopare.sona.modules.appointments.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.List;


@EqualsAndHashCode(callSuper = true)
@Data
public class ProfessionalSchedulesDto extends ScheduleBaseDto {
    //
    @NotNull
    private List<@NotNull LocalDate> dates;

}
