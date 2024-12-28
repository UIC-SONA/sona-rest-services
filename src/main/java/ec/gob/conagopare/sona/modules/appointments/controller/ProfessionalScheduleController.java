package ec.gob.conagopare.sona.modules.appointments.controller;

import ec.gob.conagopare.sona.modules.appointments.dto.ProfessionalScheduleDto;
import ec.gob.conagopare.sona.modules.appointments.models.ProfessionalSchedule;
import ec.gob.conagopare.sona.modules.appointments.service.ProfessionalScheduleService;
import io.github.luidmidev.springframework.data.crud.core.controllers.CrudController;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Getter
@RestController
@RequestMapping("/professional-schedules")
@RequiredArgsConstructor
public class ProfessionalScheduleController implements CrudController<ProfessionalSchedule, ProfessionalScheduleDto, Long, ProfessionalScheduleService> {
    private final ProfessionalScheduleService service;

}
