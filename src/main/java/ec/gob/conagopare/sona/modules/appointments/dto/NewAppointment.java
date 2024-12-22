package ec.gob.conagopare.sona.modules.appointments.dto;

import ec.gob.conagopare.sona.modules.appointments.models.Appointment;
import ec.gob.conagopare.sona.modules.user.models.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
public class NewAppointment {

    private LocalDate date;

    private Integer hour;

    private Appointment.Type type;

    private Long professionalId;

}
