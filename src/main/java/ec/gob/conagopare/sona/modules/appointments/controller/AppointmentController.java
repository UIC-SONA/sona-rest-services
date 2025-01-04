package ec.gob.conagopare.sona.modules.appointments.controller;

import ec.gob.conagopare.sona.application.common.schemas.Message;
import ec.gob.conagopare.sona.modules.appointments.dto.AppoimentRange;
import ec.gob.conagopare.sona.modules.appointments.dto.CancelAppointment;
import ec.gob.conagopare.sona.modules.appointments.dto.NewAppointment;
import ec.gob.conagopare.sona.modules.appointments.models.Appointment;
import ec.gob.conagopare.sona.modules.appointments.service.AppointmentService;
import io.github.luidmidev.springframework.data.crud.core.controllers.ReadController;
import io.github.luidmidev.springframework.data.crud.core.utils.CrudUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@RestController
@RequiredArgsConstructor
@RequestMapping("/appointment")
public class AppointmentController implements ReadController<Appointment, Long, AppointmentService> {

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
            @RequestBody CancelAppointment cancelAppointment,
            @AuthenticationPrincipal Jwt jwt
    ) {
        service.cancel(cancelAppointment, jwt);
        return ResponseEntity.ok(new Message("Cita cancelada correctamente"));
    }

    @GetMapping("/self")
    public ResponseEntity<Page<Appointment>> selfAppointments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false) String[] properties,
            @RequestParam(required = false) Sort.Direction direction,
            @RequestParam(required = false) MultiValueMap<String, String> params,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var pageable = CrudUtils.resolvePage(size, page, direction, properties);
        return ResponseEntity.ok(service.selfAppointments(search, pageable, params, jwt));
    }

    @GetMapping("/professional/{professionalId}")
    public ResponseEntity<List<AppoimentRange>> professionalAppointmentsDates(
            @PathVariable long professionalId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(service.professionalAppointmentRanges(professionalId, from, to));
    }

}
