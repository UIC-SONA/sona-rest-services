package ec.gob.conagopare.sona.modules.appointments.repository;

import ec.gob.conagopare.sona.modules.appointments.models.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {


}
