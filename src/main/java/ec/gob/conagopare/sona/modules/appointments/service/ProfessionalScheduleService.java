package ec.gob.conagopare.sona.modules.appointments.service;

import ec.gob.conagopare.sona.modules.appointments.dto.ProfessionalScheduleDto;
import ec.gob.conagopare.sona.modules.appointments.models.ProfessionalSchedule;
import ec.gob.conagopare.sona.modules.appointments.repository.ProfessionalScheduleRepository;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.springframework.data.crud.jpa.services.JpaCrudService;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProfessionalScheduleService extends JpaCrudService<ProfessionalSchedule, ProfessionalScheduleDto, Long, ProfessionalScheduleRepository> {

    private static final int MAX_DAYS_RANGE = 365;

    private final UserService userService;

    protected ProfessionalScheduleService(ProfessionalScheduleRepository repository, EntityManager entityManager, UserService userService) {
        super(repository, ProfessionalSchedule.class, entityManager);
        this.userService = userService;
    }

    @Override
    protected void mapModel(ProfessionalScheduleDto dto, ProfessionalSchedule model) {

        var date = dto.getDate();
        var fromHour = dto.getFromHour();
        var toHour = dto.getToHour();
        var professionalId = dto.getProfessionalId();

        var isOverlapping = model.isNew()
                ? repository.existsOverlappingSchedule(professionalId, date, fromHour, toHour)
                : repository.existsOverlappingScheduleExcludingId(professionalId, date, fromHour, toHour, model.getId());

        if (isOverlapping) {
            throw ApiError.badRequest("El horario que intenta registrar se superpone con otro horario del profesional");
        }

        var user = userService.find(dto.getProfessionalId());

        if (!user.is(Authority.PROFESSIONAL)) {
            throw ApiError.badRequest("El usuario no es un profesional, no puede tener horarios de atención");
        }

        model.setProfessional(user);
        model.setDate(date);
        model.setFromHour(fromHour);
        model.setToHour(toHour);
    }

    @Override
    protected void onBeforeDelete(ProfessionalSchedule model) {
        if (model.getDate().isBefore(LocalDate.now())) {
            return;
        }

        boolean hasActiveAppointments = repository.existsActiveAppointmentsInSchedule(
                model.getProfessional().getId(),
                model.getDate(),
                model.getFromHour(),
                model.getToHour()
        );

        if (hasActiveAppointments) {
            throw ApiError.badRequest("No se puede eliminar un horario que tiene citas activas");
        }
    }

    @Override
    protected Page<ProfessionalSchedule> search(String search, Pageable pageable, MultiValueMap<String, String> params) {
        throw ApiError.badRequest("Filtro no soportado");
    }

    public List<ProfessionalSchedule> getSchedulesByProfessional(Long professionalId, LocalDate from, LocalDate to) {

        if (from.isAfter(to)) {
            throw ApiError.badRequest("La fecha de inicio no puede ser mayor que la fecha de fin");
        }

        var diff = to.toEpochDay() - from.toEpochDay();

        if (diff > MAX_DAYS_RANGE) {
            throw ApiError.badRequest("El rango de fechas no puede ser mayor a " + MAX_DAYS_RANGE + " años");
        }

        return repository.getSchedulesByProfessional(professionalId, from, to);
    }
}