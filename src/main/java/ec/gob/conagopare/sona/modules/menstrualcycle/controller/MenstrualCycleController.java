package ec.gob.conagopare.sona.modules.menstrualcycle.controller;

import ec.gob.conagopare.sona.application.common.schemas.Message;
import ec.gob.conagopare.sona.modules.menstrualcycle.dto.CycleDetails;
import ec.gob.conagopare.sona.modules.menstrualcycle.models.CycleData;
import ec.gob.conagopare.sona.modules.menstrualcycle.services.MenstrualCycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/menstrual-cycle")
@RequiredArgsConstructor
public class MenstrualCycleController {

    private final MenstrualCycleService service;

    @PostMapping("/details")
    public ResponseEntity<Message> saveCycleDetails(
            @RequestBody CycleDetails cycleDetails,
            @AuthenticationPrincipal Jwt jwt
    ) {
        service.saveCycleDetails(cycleDetails, jwt);
        return ResponseEntity.ok(new Message("Detalles de ciclo guardados correctamente"));
    }

    @GetMapping
    public ResponseEntity<CycleData> getCycleData(@AuthenticationPrincipal Jwt jwt) {
        var cycleData = service.getCycle(jwt);
        return ResponseEntity.ok(cycleData);
    }


    @PostMapping("/period-logs")
    public ResponseEntity<Message> savePeriodDates(
            @RequestBody List<LocalDate> periodDates,
            @AuthenticationPrincipal Jwt jwt
    ) {
        service.savePeriodDates(periodDates, jwt);
        return ResponseEntity.ok(new Message("Fechas de periodo guardadas correctamente"));
    }
}
