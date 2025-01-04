package ec.gob.conagopare.sona.modules.appointments.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelAppointment {

    @NotNull
    private Long appointmentId;

    @NotNull(message = "Especifique el motivo de la cancelación")
    @NotEmpty(message = "Especifique el motivo de la cancelación")
    @Size(min = 5, max = 1000, message = "El motivo de la cancelación debe tener entre 5 y 1000 caracteres")
    private String reason;
}
