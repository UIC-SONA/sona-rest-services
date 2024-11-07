package ec.gob.conagopare.sona.modules.menstrualcalendar.controller;

import ec.gob.conagopare.sona.application.common.schemas.Message;
import ec.gob.conagopare.sona.modules.menstrualcalendar.dto.MenstrualCycleDto;
import ec.gob.conagopare.sona.modules.menstrualcalendar.models.MenstrualCycle;
import ec.gob.conagopare.sona.modules.menstrualcalendar.service.MenstrualCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/menstrual-calendar")
@RequiredArgsConstructor
public class MenstrualCalendarController {

    private final MenstrualCalendarService service;


    @PreAuthorize("isAuthenticated()")
    @PostMapping("/cycle")
    public ResponseEntity<Message> saveCycle(
            @RequestBody MenstrualCycleDto cycle,
            @AuthenticationPrincipal Jwt jwt
    ) {
        service.saveCycle(cycle, jwt);
        return ResponseEntity.ok(new Message("Ciclo guardado correctamente"));
    }


    @PreAuthorize("isAuthenticated()")
    @GetMapping("/cycle")
    public ResponseEntity<MenstrualCycle> getCycle(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(service.getCycle(jwt));
    }

}
