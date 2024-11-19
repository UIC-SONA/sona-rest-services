package ec.gob.conagopare.sona.modules.menstrualcalendar.services;

import ec.gob.conagopare.sona.modules.menstrualcalendar.dto.MenstrualCycleDto;
import ec.gob.conagopare.sona.modules.menstrualcalendar.models.MenstrualCycle;
import ec.gob.conagopare.sona.modules.menstrualcalendar.repositories.MenstrualCycleRepository;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class MenstrualCalendarService {

    private final MenstrualCycleRepository menstrualCycleRepository;
    private final UserService userService;

    public void saveCycle(@Valid MenstrualCycleDto dto, Jwt jwt) {

        var user = userService.getUser(jwt);
        var cycle = menstrualCycleRepository.findByUser(user).orElseGet(() -> MenstrualCycle.builder().user(user).build());

        cycle.setCycleDuration(dto.getCycleDuration());
        cycle.setPeriodDuration(dto.getPeriodDuration());
        cycle.setLastPeriodDate(dto.getLastPeriodDate());

        menstrualCycleRepository.save(cycle);
    }

    public MenstrualCycle getCycle(Jwt jwt) {
        var user = userService.getUser(jwt);
        return menstrualCycleRepository.findByUser(user).orElseThrow(() -> ApiError.notFound("Menstrual cycle not found"));
    }

}
