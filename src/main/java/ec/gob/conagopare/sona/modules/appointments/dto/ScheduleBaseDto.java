package ec.gob.conagopare.sona.modules.appointments.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
public class ScheduleBaseDto {
    @NotNull
    @Range(min = 0, max = 24)
    protected Integer fromHour;

    @NotNull
    @Range(min = 0, max = 24)
    protected Integer toHour;

    @NotNull
    protected Long professionalId;

    @SuppressWarnings("unused")
    @AssertTrue(message = "La hora de inicio debe ser menor que la hora de fin, o cruzar la medianoche")
    public boolean isValidHours() {
        if (fromHour == null || toHour == null) {
            return true;
        }

        return fromHour < toHour;
    }
}
