package ec.gob.conagopare.sona.modules.appointments.service;

import ec.gob.conagopare.sona.modules.appointments.dto.ProfessionalScheduleDto;
import ec.gob.conagopare.sona.modules.appointments.models.ProfessionalSchedule;
import ec.gob.conagopare.sona.modules.appointments.repository.ProfessionalScheduleRepository;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.springframework.data.crud.core.filters.Filter;
import io.github.luidmidev.springframework.data.crud.jpa.services.JpaCrudService;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProfessionalScheduleService extends JpaCrudService<ProfessionalSchedule, ProfessionalScheduleDto, Long, ProfessionalScheduleRepository> {

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

        if (fromHour > toHour) {
            throw ApiError.badRequest("La hora de inicio debe ser menor a la hora de fin");
        }

        var isOverlapping = model.isNew()
                ? repository.existsOverlappingSchedule(professionalId, date, fromHour, toHour)
                : repository.existsOverlappingScheduleExcludingId(professionalId, date, fromHour, toHour, model.getId());

        if (isOverlapping) {
            throw ApiError.badRequest("El horario que intenta registrar se superpone con otro horario del profesional");
        }

        var user = userService.find(dto.getProfessionalId());

        if (!user.getAuthorities().contains(Authority.PROFESSIONAL)) {
            throw ApiError.badRequest("El usuario no es un profesional, no puede tener horarios de atención");
        }

        model.setProfessional(user);
        model.setDate(date);
        model.setFromHour(fromHour);
        model.setToHour(toHour);
    }

    @Override
    protected Page<ProfessionalSchedule> search(String search, Pageable pageable, Filter filter) {
        throw ApiError.badRequest("Filtro no soportado");
    }
}
