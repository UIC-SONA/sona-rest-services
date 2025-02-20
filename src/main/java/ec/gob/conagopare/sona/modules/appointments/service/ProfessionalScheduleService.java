package ec.gob.conagopare.sona.modules.appointments.service;

import ec.gob.conagopare.sona.modules.appointments.dto.ProfessionalScheduleDto;
import ec.gob.conagopare.sona.modules.appointments.dto.ProfessionalSchedulesDto;
import ec.gob.conagopare.sona.modules.appointments.models.ProfessionalSchedule;
import ec.gob.conagopare.sona.modules.appointments.repository.ProfessionalScheduleRepository;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.springframework.data.crud.core.services.hooks.CrudHooks;
import io.github.luidmidev.springframework.data.crud.jpa.services.JpaCrudService;
import io.github.luidmidev.springframework.web.problemdetails.ProblemDetails;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

import static ec.gob.conagopare.sona.application.Constans.ECUADOR_ZONE;

@Slf4j
@Service
@RequiredArgsConstructor
@Getter
public class ProfessionalScheduleService implements JpaCrudService<ProfessionalSchedule, ProfessionalScheduleDto, Long, ProfessionalScheduleRepository> {

    private static final int MAX_DAYS_RANGE = 365;

    private final ProfessionalScheduleRepository repository;
    private final EntityManager entityManager;
    private final UserService userService;

    @Override
    public Class<ProfessionalSchedule> getEntityClass() {
        return ProfessionalSchedule.class;
    }

    @Override
    public void mapModel(ProfessionalScheduleDto dto, ProfessionalSchedule model) {

        var startDate = ZonedDateTime.of(dto.getDate(), LocalTime.of(dto.getFromHour(), 0), ECUADOR_ZONE);
        var nowInEcuador = ZonedDateTime.now(ECUADOR_ZONE);

        if (startDate.isBefore(nowInEcuador)) {
            throw ProblemDetails.badRequest("La fecha de inicio no puede ser menor a la fecha actual");
        }

        var user = userService.find(dto.getProfessionalId());

        if (!user.isAny(Authority.LEGAL_PROFESSIONAL, Authority.MEDICAL_PROFESSIONAL)) {
            throw ProblemDetails.badRequest("El usuario no es un profesional, no puede tener horarios de atención");
        }

        var isNew = model.isNew();
        var date = dto.getDate();
        var fromHour = dto.getFromHour();
        var toHour = dto.getToHour();
        var professionalId = dto.getProfessionalId();

        var isOverlapping = isNew
                ? repository.existsOverlappingSchedule(professionalId, date, fromHour, toHour)
                : repository.existsOverlappingScheduleExcludingId(professionalId, date, fromHour, toHour, model.getId());

        if (isOverlapping) {
            throw ProblemDetails.badRequest("El horario que intenta registrar se superpone con otro horario del profesional");
        }

        if (!isNew && repository.existsActiveAppointmentsInSchedule(professionalId, model.getDate(), model.getFromHour(), model.getToHour())) {
            throw ProblemDetails.badRequest("No se puede modificar un horario que tiene citas activas");
        }

        model.setProfessional(user);
        model.setDate(date);
        model.setFromHour(fromHour);
        model.setToHour(toHour);
    }

    @Override
    public Page<ProfessionalSchedule> internalSearch(String search, Pageable pageable, MultiValueMap<String, String> params) {
        throw ProblemDetails.badRequest("Filtro no soportado");
    }

    public List<ProfessionalSchedule> getSchedulesByProfessional(Long professionalId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw ProblemDetails.badRequest("La fecha de inicio no puede ser mayor que la fecha de fin");
        }

        var diff = to.toEpochDay() - from.toEpochDay();
        if (diff > MAX_DAYS_RANGE) {
            throw ProblemDetails.badRequest("El rango de fechas no puede ser mayor a " + MAX_DAYS_RANGE + " años");
        }

        return repository.getSchedulesByProfessional(professionalId, from, to);
    }

    @Transactional
    @PreAuthorize("hasRole('admin')")
    public List<ProfessionalSchedule> createAll(@Valid ProfessionalSchedulesDto dto) {
        var user = userService.find(dto.getProfessionalId());

        if (!user.isAny(Authority.LEGAL_PROFESSIONAL, Authority.MEDICAL_PROFESSIONAL)) {
            throw ProblemDetails.badRequest("El usuario no es un profesional, no puede tener horarios de atención");
        }

        var created = new ArrayList<ProfessionalSchedule>();
        for (var dates : dto.getDates()) {
            var startDate = ZonedDateTime.of(dates, LocalTime.of(dto.getFromHour(), 0), ECUADOR_ZONE);
            var nowInEcuador = ZonedDateTime.now(ECUADOR_ZONE);

            if (startDate.isBefore(nowInEcuador)) {
                throw ProblemDetails.badRequest("La fecha de inicio no puede ser menor a la fecha actual");
            }

            var isOverlapping = repository.existsOverlappingSchedule(dto.getProfessionalId(), dates, dto.getFromHour(), dto.getToHour());

            if (isOverlapping) {
                throw ProblemDetails.badRequest("Al menos uno de los horarios que intenta registrar se superpone con otro horario del profesional");
            }

            var schedule = new ProfessionalSchedule();
            schedule.setProfessional(user);
            schedule.setDate(dates);
            schedule.setFromHour(dto.getFromHour());
            schedule.setToHour(dto.getToHour());

            created.add(repository.save(schedule));
        }
        return created;
    }


    private final CrudHooks<ProfessionalSchedule, ProfessionalScheduleDto, Long> hooks = new CrudHooks<>() {
        @Override
        public void onBeforeDelete(ProfessionalSchedule model) {
            var toHour = model.getToHour();
            var fromHour = model.getFromHour();

            var date = toHour == 24 ? model.getDate().plusDays(1) : model.getDate();
            var time = toHour == 24 ? LocalTime.of(0, 0) : LocalTime.of(toHour, 0);

            var endDate = ZonedDateTime.of(date, time, ECUADOR_ZONE);
            var nowInEcuador = ZonedDateTime.now(ECUADOR_ZONE);

            if (endDate.isBefore(nowInEcuador)) return;

            boolean hasActiveAppointments = repository.existsActiveAppointmentsInSchedule(
                    model.getProfessional().getId(),
                    model.getDate(),
                    fromHour,
                    toHour
            );

            if (hasActiveAppointments) {
                throw ProblemDetails.badRequest("No se puede eliminar un horario que tiene citas activas");
            }
        }
    };
}
