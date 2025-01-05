package ec.gob.conagopare.sona.modules.appointments.service;

import ec.gob.conagopare.sona.modules.appointments.dto.AppointmentRange;
import ec.gob.conagopare.sona.modules.appointments.dto.CancelAppointment;
import ec.gob.conagopare.sona.modules.appointments.dto.NewAppointment;
import ec.gob.conagopare.sona.modules.appointments.models.Appointment;
import ec.gob.conagopare.sona.modules.appointments.repository.AppointmentRepository;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.springframework.data.crud.jpa.services.JpaReadService;
import io.github.luidmidev.springframework.data.crud.jpa.utils.AdditionsSearch;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static ec.gob.conagopare.sona.modules.user.models.User.KEYCLOAK_ID_ATTRIBUTE;

@Slf4j
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

        log.info("Reservando cita para el profesional {} en la fecha {} y hora {} por el usuario {}", profesionalId, date, hour, jwt.getSubject());
        if (!repository.isWithinProfessionalSchedule(profesionalId, date, hour)) {
            throw ApiError.badRequest("La hora de la cita que intenta programar no está dentro del horario de atención del profesional");
        }

        var user = userService.getUser(jwt);

        if (!user.is(Authority.USER)) {
            throw ApiError.badRequest("El usuario, no puede tener citas programadas");
        }

        var profesional = userService.find(profesionalId);

        if (!profesional.isAny(Authority.LEGAL_PROFESSIONAL, Authority.MEDICAL_PROFESSIONAL)) {
            throw ApiError.badRequest("El usuario no es un profesional, no puede tener citas programadas");
        }

        var appointment = Appointment.builder()
                .attendant(user)
                .professional(profesional)
                .date(date)
                .hour(hour)
                .type(newAppointment.getType())
                .build();

        return repository.save(appointment);
    }

    @PreAuthorize("isAuthenticated()")
    public void cancel(CancelAppointment cancelAppointment, Jwt jwt) {
        var user = userService.getUser(jwt);
        var userId = user.getId();
        var appointment = find(cancelAppointment.getAppointmentId());

        assert userId != null;
        if (!userId.equals(appointment.getAttendant().getId())) {
            throw ApiError.forbidden("No tiene permisos para cancelar esta cita");
        }

        if (appointment.isCanceled()) {
            throw ApiError.badRequest("La cita ya ha sido cancelada");
        }

        appointment.setCanceled(true);
        appointment.setCancelationReason(cancelAppointment.getReason());

        repository.save(appointment);
    }

    public Page<Appointment> selfAppointments(String search, Pageable pageable, MultiValueMap<String, String> params, Jwt jwt) {
        params.add(KEYCLOAK_ID_ATTRIBUTE, jwt.getSubject());
        return doPage(search, pageable, params);
    }

    @Override
    protected Page<Appointment> search(String search, Pageable pageable, MultiValueMap<String, String> params) {
        var additions = new AdditionsSearch<Appointment>();

        additions.and((root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();

            var keycloakId = params.getFirst(KEYCLOAK_ID_ATTRIBUTE);
            if (keycloakId != null) {
                predicates.add(cb.equal(root.join("attendant").get(KEYCLOAK_ID_ATTRIBUTE), keycloakId));
            }

            var professionalId = params.getFirst("professionalId");
            if (professionalId != null) {
                predicates.add(cb.equal(root.join("professional").get("id"), Long.parseLong(professionalId)));
            }

            var canceled = params.getFirst("canceled");
            if (canceled != null) {
                predicates.add(cb.equal(root.get("canceled"), Boolean.parseBoolean(canceled)));
            }

            var type = params.getFirst("type");
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), Appointment.Type.valueOf(type)));
            }

            var from = params.getFirst("from");
            var to = params.getFirst("to");

            if (from != null && to != null) {
                predicates.add(cb.between(root.get("date"), LocalDate.parse(from), LocalDate.parse(to)));
            } else if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), LocalDate.parse(from)));
            } else if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), LocalDate.parse(to)));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        });

        return search(search, pageable, additions);
    }

    @PreAuthorize("isAuthenticated()")
    public List<AppointmentRange> professionalAppointmentRanges(long professionalId, LocalDate from, LocalDate to) {
        return repository.getProfessionalAppointmentsRanges(professionalId, from, to);
    }
}
