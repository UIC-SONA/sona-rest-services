package ec.gob.conagopare.sona.modules.menstrualcycle.services;

import ec.gob.conagopare.sona.modules.menstrualcycle.dto.CycleDetails;
import ec.gob.conagopare.sona.modules.menstrualcycle.models.CycleData;
import ec.gob.conagopare.sona.modules.menstrualcycle.repositories.MenstrualCycleRepository;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.springframework.web.problemdetails.ProblemDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class MenstrualCycleService {

    private final MenstrualCycleRepository repository;
    private final UserService userService;

    @PreAuthorize("isAuthenticated()")
    public CycleData getCycle(Jwt jwt) {
        log.info("Getting menstrual cycle data for user {}", jwt.getSubject());
        var user = userService.getUser(jwt);
        return repository.findByUser(user).orElseThrow(() -> ProblemDetails.notFound("Menstrual cycle not found"));
    }

    private CycleData getCycleOrNew(Jwt jwt) {
        log.info("Getting or creating menstrual cycle data for user {}", jwt.getSubject());
        var user = userService.getUser(jwt);
        return repository.findByUser(user).orElseGet(() -> CycleData
                .builder()
                .user(user)
                .cycleLength(28)
                .periodDuration(5)
                .build()
        );
    }

    @PreAuthorize("isAuthenticated()")
    public void saveCycleDetails(@Valid CycleDetails cycleDetails, Jwt jwt) {
        log.info("Saving menstrual cycle details for user {}", jwt.getSubject());

        var cycle = getCycleOrNew(jwt);

        cycle.setCycleLength(cycleDetails.getCycleLength());
        cycle.setPeriodDuration(cycleDetails.getPeriodDuration());

        repository.save(cycle);
    }

    @PreAuthorize("isAuthenticated()")
    public void savePeriodDates(@Valid List<LocalDate> periodDates, Jwt jwt) {
        log.info("Saving menstrual cycle period dates for user {}", jwt.getSubject());

        var cycle = getCycleOrNew(jwt);

        cycle.setPeriodDates(periodDates);
        repository.save(cycle);
    }
}
