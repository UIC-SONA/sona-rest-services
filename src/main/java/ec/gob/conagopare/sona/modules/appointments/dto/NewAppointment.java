package ec.gob.conagopare.sona.modules.appointments.dto;

import ec.gob.conagopare.sona.modules.appointments.models.Appointment;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.time.LocalDate;

@Data
public class NewAppointment {

    @NotNull
    @FutureOrPresent
    private LocalDate date;

    @NotNull
    @Range(min = 0, max = 23)
    private Integer hour;

    @NotNull
    private Appointment.Type type;

    @NotNull
    private Long professionalId;
}
