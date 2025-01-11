package ec.gob.conagopare.sona.modules.appointments.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.time.LocalDate;
import java.util.List;


@Data
public class ProfessionalSchedulesDto {
    //
    @NotNull
    private List<@NotNull LocalDate> dates;

    @NotNull
    @Range(min = 0, max = 24)
    private Integer fromHour;

    @NotNull
    @Range(min = 0, max = 24)
    private Integer toHour;

    @NotNull
    private Long professionalId;

    @SuppressWarnings("unused")
    @AssertTrue(message = "La hora de inicio debe ser menor que la hora de fin, o cruzar la medianoche")
    public boolean isValidHours() {
        if (fromHour == null || toHour == null) {
            return true;
        }

        return fromHour < toHour;
    }
}
