package ec.gob.conagopare.sona.modules.appointments.controller;

import ec.gob.conagopare.sona.modules.appointments.dto.ProfessionalScheduleDto;
import ec.gob.conagopare.sona.modules.appointments.dto.ProfessionalSchedulesDto;
import ec.gob.conagopare.sona.modules.appointments.models.ProfessionalSchedule;
import ec.gob.conagopare.sona.modules.appointments.service.ProfessionalScheduleService;
import io.github.luidmidev.springframework.data.crud.core.http.controllers.CrudController;
import io.github.luidmidev.springframework.data.crud.core.http.controllers.ExportController;
import io.github.luidmidev.springframework.data.crud.core.http.export.SpreadSheetExporter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@RestController
@RequestMapping("/professional-schedule")
@RequiredArgsConstructor
public class ProfessionalScheduleController implements CrudController<ProfessionalSchedule, ProfessionalScheduleDto, Long, ProfessionalScheduleService>, ExportController<Long, ProfessionalScheduleService> {

    private final ProfessionalScheduleService service;
    private final SpreadSheetExporter exporter;

    @PostMapping("/all")
    public ResponseEntity<List<ProfessionalSchedule>> createAll(
            @RequestBody ProfessionalSchedulesDto schedules
    ) {
        return ResponseEntity.ok(service.createAll(schedules));
    }

    @GetMapping("/professional/{professionalId}")
    public ResponseEntity<List<ProfessionalSchedule>> getSchedulesByProfessional(
            @PathVariable Long professionalId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(service.getSchedulesByProfessional(professionalId, from, to));
    }

}
