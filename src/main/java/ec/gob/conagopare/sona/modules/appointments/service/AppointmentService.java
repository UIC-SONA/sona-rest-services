package ec.gob.conagopare.sona.modules.appointments.service;

import ec.gob.conagopare.sona.modules.appointments.dto.AppointmentRange;
import ec.gob.conagopare.sona.modules.appointments.dto.CancelAppointment;
import ec.gob.conagopare.sona.modules.appointments.dto.NewAppointment;
import ec.gob.conagopare.sona.modules.appointments.models.Appointment;
import ec.gob.conagopare.sona.modules.appointments.repository.AppointmentRepository;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import ec.gob.conagopare.sona.modules.user.models.User;
import ec.gob.conagopare.sona.modules.content.services.NotificationService;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.springframework.data.crud.jpa.services.JpaSpecificationReadService;
import io.github.luidmidev.springframework.data.crud.jpa.utils.AdditionsSearch;
import io.github.luidmidev.springframework.data.crud.jpa.utils.JpaSmartSearch;
import io.github.luidmidev.springframework.web.problemdetails.ProblemDetails;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ec.gob.conagopare.sona.application.Constans.ECUADOR_ZONE;
import static ec.gob.conagopare.sona.modules.appointments.models.Appointment.ATTENDANT_ATTRIBUTE;
import static ec.gob.conagopare.sona.modules.appointments.models.Appointment.PROFESSIONAL_ATTRIBUTE;
import static ec.gob.conagopare.sona.modules.user.models.User.KEYCLOAK_ID_ATTRIBUTE;

@Slf4j
@Service
@RequiredArgsConstructor
@Getter
public class AppointmentService implements JpaSpecificationReadService<Appointment, Long, AppointmentRepository> {

    private static final Authority[] PRIVILEGED_AUTHORITIES = {Authority.ADMIN, Authority.ADMINISTRATIVE};

    private final AppointmentRepository repository;
    private final EntityManager entityManager;
    private final UserService userService;
    private final NotificationService notificationService;

    @Override
    public Class<Appointment> getEntityClass() {
        return Appointment.class;
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
            throw ProblemDetails.badRequest("Ya existe una cita programada a esa hora");
        }

        if (!repository.isWithinProfessionalSchedule(profesionalId, date, hour)) {
            throw ProblemDetails.badRequest("La hora de la cita que intenta programar no está dentro del horario de atención del profesional");
        }

        var user = userService.getUser(jwt);

        if (!user.is(Authority.USER)) {
            throw ProblemDetails.badRequest("El usuario, no puede tener citas programadas");
        }

        var nowInEcuador = ZonedDateTime.now(ECUADOR_ZONE);
        if (repository.countFutureAppointments(user.getId(), nowInEcuador.toLocalDate(), nowInEcuador.getHour()) >= 2) {
            throw ProblemDetails.badRequest("No puede tener más de dos a futuro activas, si requiere de agender una cita pruebe cancelando una de las activas");
        }

        var profesional = userService.find(profesionalId);

        if (!profesional.isAny(Authority.LEGAL_PROFESSIONAL, Authority.MEDICAL_PROFESSIONAL)) {
            throw ProblemDetails.badRequest("El usuario seleccionado no es un profesional, no puede tener citas programadas");
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
            throw ProblemDetails.forbidden("No tiene permisos para cancelar esta cita");
        }

        if (appointment.isCanceled()) {
            throw ProblemDetails.badRequest("La cita ya ha sido cancelada");
        }

        appointment.setCanceled(true);
        appointment.setCancellationReason(cancelAppointment.getReason());

        repository.save(appointment);
    }


    @Override
    public Page<Appointment> internalSearch(String search, Pageable pageable) {
        var additions = new AdditionsSearch<Appointment>();
        additions.addJoins(ATTENDANT_ATTRIBUTE, PROFESSIONAL_ATTRIBUTE);
        return JpaSmartSearch.search(entityManager, search, pageable, additions, getEntityClass());
    }


    @PreAuthorize("isAuthenticated()")
    public List<Appointment> list(String search, Sort sort, MultiValueMap<String, String> filters) {
        var unpaged = Pageable.unpaged(sort);
        return doPage(search, unpaged, filters).getContent();
    }


    @Override
    @SuppressWarnings("java:S3776")
    public Page<Appointment> internalSearch(String search, Pageable pageable, MultiValueMap<String, String> filters) {
        var additions = new AdditionsSearch<Appointment>();
        additions.addJoins(ATTENDANT_ATTRIBUTE, PROFESSIONAL_ATTRIBUTE);
        additions.and((root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();

            var keycloakId = filters.getFirst(KEYCLOAK_ID_ATTRIBUTE);
            if (keycloakId != null) {
                predicates.add(cb.equal(root.join(ATTENDANT_ATTRIBUTE).get(KEYCLOAK_ID_ATTRIBUTE), keycloakId));
            }

                var userId = filters.getFirst("userId");
            if (userId != null) {
                predicates.add(cb.equal(root.join(ATTENDANT_ATTRIBUTE).get("id"), Long.parseLong(userId)));
            }

            var professionalId = filters.getFirst("professionalId");
            if (professionalId != null) {
                predicates.add(cb.equal(root.join(PROFESSIONAL_ATTRIBUTE).get("id"), Long.parseLong(professionalId)));
            } else {
                var professionalType = filters.getFirst("professionalType");
                if (professionalType != null) {
                    predicates.add(cb.equal(root.join(PROFESSIONAL_ATTRIBUTE).join("authorities"), Authority.valueOf(professionalType)));
                }
            }

            var canceled = filters.getFirst("canceled");
            if (canceled != null) {
                predicates.add(cb.equal(root.get("canceled"), Boolean.parseBoolean(canceled)));
            }

            var type = filters.getFirst("type");
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), Appointment.Type.valueOf(type)));
            }

            var from = filters.getFirst("from");
            var to = filters.getFirst("to");

            if (from != null && to != null) {
                predicates.add(cb.between(root.get("date"), LocalDate.parse(from), LocalDate.parse(to)));
            } else if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), LocalDate.parse(from)));
            } else if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), LocalDate.parse(to)));
            }

            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        });

        return internalSearch(search, pageable, additions);
    }


    @PreAuthorize("isAuthenticated()")
    public Page<Appointment> selfAppointments(String search, Pageable pageable, MultiValueMap<String, String> params, Jwt jwt) {
        params.add(KEYCLOAK_ID_ATTRIBUTE, jwt.getSubject());
        return doPage(search, pageable, params);
    }

    @PreAuthorize("isAuthenticated()")
    public List<AppointmentRange> professionalAppointmentRanges(long professionalId, LocalDate from, LocalDate to) {
        return repository.getProfessionalAppointmentsRanges(professionalId, from, to);
    }

    @Override
    public Specification<Appointment> processSpecification(Specification<Appointment> spec) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var jwt = (Jwt) authentication.getPrincipal();

        var user = userService.getUser(jwt);
        var userId = user.getId();
        if (!user.isAny(PRIVILEGED_AUTHORITIES)) {
            return spec.and((root, query, cb) -> cb.or(
                    cb.equal(root.join(ATTENDANT_ATTRIBUTE).get("id"), userId),
                    cb.equal(root.join(PROFESSIONAL_ATTRIBUTE).get("id"), userId)
            ));
        }

        return spec;
    }


    @Scheduled(cron = "0 0 0 * * *")
    public void remindAppoinments() {
        var now = LocalDate.now();
        var tomorrowAppointments = repository.getAppointmentsByDate(now.plusDays(1));
        notificationService.send(
                mapAttendantsIds(tomorrowAppointments),
                "Recordatorio de un evento en tu calendario menstrual",
                "Te recordamos que te prepares, ¡no olvides asistir!"
        );
    }

    private static List<Long> mapAttendantsIds(Collection<Appointment> appointments) {
        return appointments.stream().map(Appointment::getAttendant).map(User::getId).distinct().toList();
    }

}
