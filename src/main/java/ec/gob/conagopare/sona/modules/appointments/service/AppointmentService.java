package ec.gob.conagopare.sona.modules.appointments.service;

import ec.gob.conagopare.sona.modules.appointments.dto.NewAppointment;
import ec.gob.conagopare.sona.modules.appointments.models.Appointment;
import ec.gob.conagopare.sona.modules.appointments.repository.AppointmentRepository;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.springframework.data.crud.core.filters.Filter;
import io.github.luidmidev.springframework.data.crud.jpa.services.JpaReadService;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class AppointmentService extends JpaReadService<Appointment, Long, AppointmentRepository> {

    private final UserService userService;

    protected AppointmentService(AppointmentRepository repository, EntityManager entityManager, UserService userService) {
        super(repository, Appointment.class, entityManager);
        this.userService = userService;
    }


    @PreAuthorize("isAuthenticated()")
    public Appointment program(
            @Valid NewAppointment newAppointment,
            Jwt jwt
    ) {
        var profesionalId = newAppointment.getProfessionalId();
        var date = newAppointment.getDate();
        var hour = newAppointment.getHour();

        if (repository.existsAppointmentAtHour(profesionalId, date, hour)) {
            throw ApiError.badRequest("Ya existe una cita programada a esa hora");
        }

        if (repository.isWithinProfessionalSchedule(profesionalId, date, hour)) {
            throw ApiError.badRequest("La hora de la cita que intenta programar no está dentro del horario de atención del profesional");
        }

        var user = userService.getUser(jwt);

        if (!user.getAuthorities().contains(Authority.USER)) {
            throw ApiError.badRequest("El usuario no es un usuario, no puede tener citas programadas");
        }

        var profesional = userService.find(profesionalId);

        if (!profesional.getAuthorities().contains(Authority.PROFESSIONAL)) {
            throw ApiError.badRequest("El usuario no es un profesional, no puede tener citas programadas");
        }

        var appointment = Appointment.builder()
                .attendant(user)
                .professional(profesional)
                .date(date)
                .hour(hour)
                .build();

        return repository.save(appointment);
    }

    @Override
    protected Page<Appointment> search(String search, Pageable pageable, Filter filter) {
        throw ApiError.badRequest("Filtro no soportado");
    }


    @PreAuthorize("isAuthenticated()")
    public void cancel(Long appointmentId, Jwt jwt) {
        var user = userService.getUser(jwt);
        var userId = user.getId();
        var appointment = find(appointmentId);

        assert userId != null;
        if (!userId.equals(appointment.getAttendant().getId())) {
            throw ApiError.forbidden("No tiene permisos para cancelar esta cita");
        }

        repository.delete(appointment);
    }
}
