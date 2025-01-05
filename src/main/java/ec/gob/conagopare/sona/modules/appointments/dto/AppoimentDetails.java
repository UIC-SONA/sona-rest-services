package ec.gob.conagopare.sona.modules.appointments.dto;

import ec.gob.conagopare.sona.modules.appointments.models.Appointment;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AppoimentDetails {
    private final LocalDateTime from;
    private final LocalDateTime to;
    private final Appointment.Type type;

    public AppoimentDetails(LocalDate date, Integer fromHour, Integer toHour, Appointment.Type type) {
        this.from = date.atTime(fromHour, 0);
        this.to = toHour == 24 ? date.plusDays(1).atStartOfDay() : date.atTime(toHour, 0);
        this.type = type;
    }

    public AppoimentDetails(LocalDate date, Integer fromHour, Integer toHour) {
        this(date, fromHour, toHour, null);
    }
}