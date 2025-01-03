package ec.gob.conagopare.sona.modules.appointments.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AppoimentRange {
    private final LocalDateTime from;
    private final LocalDateTime to;

    public AppoimentRange(LocalDate day, Integer fromHour, Integer toHour) {
        this.from = day.atTime(fromHour, 0);
        this.to = day.atTime(toHour, 0);
    }

}