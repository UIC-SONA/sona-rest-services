package ec.gob.conagopare.sona.modules.appointments.controller;

import ec.gob.conagopare.sona.application.common.schemas.Message;
import ec.gob.conagopare.sona.modules.appointments.dto.NewAppointment;
import ec.gob.conagopare.sona.modules.appointments.models.Appointment;
import ec.gob.conagopare.sona.modules.appointments.service.AppointmentService;
import io.github.luidmidev.springframework.data.crud.core.controllers.ReadController;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Getter
@RestController
@RequiredArgsConstructor
@RequestMapping("/appointments")
public class AppoinmentController implements ReadController<Appointment, Long, AppointmentService> {

    private final AppointmentService service;

    @PostMapping("/program")
    public ResponseEntity<Appointment> program(
            @RequestBody NewAppointment newAppointment,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(service.program(newAppointment, jwt));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Message> cancel(
            @RequestParam Long appointmentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        service.cancel(appointmentId, jwt);
        return ResponseEntity.ok(new Message("Cita cancelada correctamente"));
    }


}
