package ec.gob.conagopare.sona.modules.appointments.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CancelAppointment {

    @NotNull
    private Long appointmentId;

    @NotNull(message = "Especifique el motivo de la cancelación")
    @NotEmpty(message = "Especifique el motivo de la cancelación")
    private String reason;
}
