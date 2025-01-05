package ec.gob.conagopare.sona.modules.appointments.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AppointmentRange {
    private final LocalDateTime from;
    private final LocalDateTime to;

    public AppointmentRange(LocalDate date, Integer fromHour, Integer toHour) {
        this.from = date.atTime(fromHour, 0);
        this.to = toHour == 24 ? date.plusDays(1).atStartOfDay() : date.atTime(toHour, 0);
    }
}