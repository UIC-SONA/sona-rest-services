package ec.gob.conagopare.sona.modules.menstrualcycle.services;

import ec.gob.conagopare.sona.modules.menstrualcycle.dto.CycleDetails;
import ec.gob.conagopare.sona.modules.menstrualcycle.models.CycleData;
import ec.gob.conagopare.sona.modules.menstrualcycle.repositories.MenstrualCycleRepository;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class MenstrualCycleService {

    private final MenstrualCycleRepository menstrualCycleRepository;
    private final UserService userService;

    @PreAuthorize("isAuthenticated()")
    public CycleData getCycle(Jwt jwt) {
        var user = userService.getUser(jwt);
        return menstrualCycleRepository.findByUser(user).orElseThrow(() -> ApiError.notFound("Menstrual cycle not found"));
    }

    @PreAuthorize("isAuthenticated()")
    public void saveCycleDetails(@Valid CycleDetails cycleDetails, Jwt jwt) {
        var cycle = getCycle(jwt);

        cycle.setCycleLength(cycleDetails.getCycleLength());
        cycle.setPeriodDuration(cycleDetails.getPeriodDuration());

        menstrualCycleRepository.save(cycle);
    }

    @PreAuthorize("isAuthenticated()")
    public void savePeriodDates(@Valid List<LocalDate> periodDates, Jwt jwt) {
        var cycle = getCycle(jwt);

        cycle.setPeriodDates(periodDates);

        menstrualCycleRepository.save(cycle);
    }
}
