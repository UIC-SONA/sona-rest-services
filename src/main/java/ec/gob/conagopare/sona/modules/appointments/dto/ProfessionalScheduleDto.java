package ec.gob.conagopare.sona.modules.appointments.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.time.LocalDate;


@Data
public class ProfessionalScheduleDto {
    //
    @FutureOrPresent
    private LocalDate date;

    @Range(min = 0, max = 23)
    private Integer fromHour;

    @Range(min = 0, max = 23)
    private Integer toHour;

    @NotNull
    private Long professionalId;
}
