package ec.gob.conagopare.sona.modules.appointments.service;

import ec.gob.conagopare.sona.modules.appointments.dto.CancelAppointment;
import ec.gob.conagopare.sona.modules.appointments.dto.NewAppointment;
import ec.gob.conagopare.sona.modules.appointments.models.Appointment;
import ec.gob.conagopare.sona.modules.appointments.repository.AppointmentRepository;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import ec.gob.conagopare.sona.modules.user.models.User;
import ec.gob.conagopare.sona.modules.user.service.NotificationService;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.springframework.web.problemdetails.ProblemDetailsException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestAppointmentService {

    @InjectMocks
    private AppointmentService service;

    @Mock
    private AppointmentRepository repository;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    private static Jwt jwt;
    private static User user;

    private static final List<MockedStatic<?>> MOCKED_STATICS = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        var keycloakId = "keycloak-id";
        jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(keycloakId)
                .build();

        user = User.builder()
                .id(1L)
                .keycloakId(keycloakId)
                .authorities(Set.of(Authority.USER))
                .build();

        var authentication = new JwtAuthenticationToken(jwt);
        var staticMock = Mockito.mockStatic(SecurityContextHolder.class);
        staticMock.when(SecurityContextHolder::getContext).thenReturn(new SecurityContextImpl(authentication));
        MOCKED_STATICS.add(staticMock);

    }

    @AfterAll
    static void tearDown() {
        MOCKED_STATICS.forEach(MockedStatic::close);
    }

    @AfterEach
    void resetMocks() {
        reset(repository, userService, notificationService);
        user.setAuthorities(Set.of(Authority.USER));
    }

    @Test
    void program_CuandoLosDatosSonCorrectos_DebeRetornarLaCitaProgramada() {

        // Arrange
        var professionalId = 2L;

        var appointmentDate = LocalDate.of(2021, 10, 10);
        var appointmentHour = 10;
        var appointmentType = Appointment.Type.VIRTUAL;

        var newAppointment = new NewAppointment();
        newAppointment.setDate(appointmentDate);
        newAppointment.setHour(appointmentHour);
        newAppointment.setProfessionalId(professionalId);
        newAppointment.setType(appointmentType);

        var professional = User.builder()
                .id(professionalId)
                .firstName("John")
                .lastName("Doe")
                .authorities(Set.of(Authority.MEDICAL_PROFESSIONAL))
                .build();

        // Mocks
        when(repository.existsAppointmentAtHour(professionalId, appointmentDate, appointmentHour)).thenReturn(false);
        when(repository.isWithinProfessionalSchedule(professionalId, appointmentDate, appointmentHour)).thenReturn(true);
        when(repository.countFutureAppointments(eq(user.getId()), any(), any())).thenReturn(0L);
        when(userService.getUser(jwt)).thenReturn(user);
        when(userService.find(professionalId)).thenReturn(professional);
        when(repository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = service.program(newAppointment, jwt);

        // Assert
        assertNotNull(result, "La cita programada no debe ser nula");
        assertEquals(professional, result.getProfessional(), "El profesional de la cita no es el esperado");
        assertEquals(user, result.getAttendant(), "El usuario de la cita no es el esperado");
        assertEquals(appointmentDate, result.getDate(), "La fecha de la cita no es la esperada");
        assertEquals(appointmentHour, result.getHour(), "La hora de la cita no es la esperada");
        assertEquals(appointmentType, result.getType(), "El tipo de cita no es el esperado");
    }

    @Test
    void program_CuandoElProfesionalYaTieneCitaAEsaHora_DebeLanzarExcepcion() {

        // Arrange
        var professionalId = 2L;

        var appointmentDate = LocalDate.of(2021, 10, 10);
        var appointmentHour = 10;
        var appointmentType = Appointment.Type.VIRTUAL;

        var newAppointment = new NewAppointment();
        newAppointment.setDate(appointmentDate);
        newAppointment.setHour(appointmentHour);
        newAppointment.setProfessionalId(professionalId);
        newAppointment.setType(appointmentType);

        // Mocks
        when(repository.existsAppointmentAtHour(professionalId, appointmentDate, appointmentHour)).thenReturn(true);

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.program(newAppointment, jwt), "Se espera que se lance una excepción si el profesional ya tiene una cita programada a esa hora");
        var body = exception.getBody();
        assertEquals(400, body.getStatus(), "El código de estado de la excepción no es el esperado");
    }

    @Test
    void program_CuandoElProfesionalNoTieneHorarioDeAtencionAEsaHora_DebeLanzarExcepcion() {

        // Arrange
        var professionalId = 2L;

        var appointmentDate = LocalDate.of(2021, 10, 10);
        var appointmentHour = 10;
        var appointmentType = Appointment.Type.VIRTUAL;

        var newAppointment = new NewAppointment();
        newAppointment.setDate(appointmentDate);
        newAppointment.setHour(appointmentHour);
        newAppointment.setProfessionalId(professionalId);
        newAppointment.setType(appointmentType);

        // Mocks
        when(repository.existsAppointmentAtHour(professionalId, appointmentDate, appointmentHour)).thenReturn(false);
        when(repository.isWithinProfessionalSchedule(professionalId, appointmentDate, appointmentHour)).thenReturn(false);

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.program(newAppointment, jwt), "Se espera que se lance una excepción si el profesional no tiene horario a esa hora");
        var body = exception.getBody();
        assertEquals(400, body.getStatus(), "El código de estado de la excepción no es el esperado");
    }

    @Test
    void program_CuandoElUsuarioNoEsUnUsuario_DebeLanzarExcepcion() {

        // Arrange
        var professionalId = 2L;

        var appointmentDate = LocalDate.of(2021, 10, 10);
        var appointmentHour = 10;
        var appointmentType = Appointment.Type.VIRTUAL;

        var newAppointment = new NewAppointment();
        newAppointment.setDate(appointmentDate);
        newAppointment.setHour(appointmentHour);
        newAppointment.setProfessionalId(professionalId);
        newAppointment.setType(appointmentType);

        var professional = User.builder()
                .id(professionalId)
                .firstName("John")
                .lastName("Doe")
                .authorities(Set.of(Authority.MEDICAL_PROFESSIONAL))
                .build();

        // Mocks
        when(repository.existsAppointmentAtHour(professionalId, appointmentDate, appointmentHour)).thenReturn(false);
        when(repository.isWithinProfessionalSchedule(professionalId, appointmentDate, appointmentHour)).thenReturn(true);
        when(userService.getUser(jwt)).thenReturn(professional);

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.program(newAppointment, jwt), "Se espera que se lance una excepción si el usuario no es un usuario");
        var body = exception.getBody();
        assertEquals(400, body.getStatus(), "El código de estado de la excepción no es el esperado");
    }

    @Test
    void program_CuandoElProfesionalNoEsUnProfesional_DebeLanzarExcepcion() {

        // Arrange
        var professionalId = 2L;

        var appointmentDate = LocalDate.of(2021, 10, 10);
        var appointmentHour = 10;
        var appointmentType = Appointment.Type.VIRTUAL;

        var newAppointment = new NewAppointment();
        newAppointment.setDate(appointmentDate);
        newAppointment.setHour(appointmentHour);
        newAppointment.setProfessionalId(professionalId);
        newAppointment.setType(appointmentType);

        var professional = User.builder()
                .id(professionalId)
                .firstName("John")
                .lastName("Doe")
                .authorities(Set.of(Authority.USER))
                .build();

        // Mocks
        when(repository.existsAppointmentAtHour(professionalId, appointmentDate, appointmentHour)).thenReturn(false);
        when(repository.isWithinProfessionalSchedule(professionalId, appointmentDate, appointmentHour)).thenReturn(true);
        when(userService.getUser(jwt)).thenReturn(user);
        when(userService.find(professionalId)).thenReturn(professional);

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.program(newAppointment, jwt), "Se espera que se lance una excepción si el usuario no es un profesional");
        var body = exception.getBody();
        assertEquals(400, body.getStatus(), "El código de estado de la excepción no es el esperado");
    }

    @Test
    void program_CuandoElUsuarioTieneMasDeDosCitasProgramadas_DebeLanzarExcepcion() {

        // Arrange
        var professionalId = 2L;

        var appointmentDate = LocalDate.of(2021, 10, 10);
        var appointmentHour = 10;
        var appointmentType = Appointment.Type.VIRTUAL;

        var newAppointment = new NewAppointment();
        newAppointment.setDate(appointmentDate);
        newAppointment.setHour(appointmentHour);
        newAppointment.setProfessionalId(professionalId);
        newAppointment.setType(appointmentType);


        // Mocks
        when(repository.existsAppointmentAtHour(professionalId, appointmentDate, appointmentHour)).thenReturn(false);
        when(repository.isWithinProfessionalSchedule(professionalId, appointmentDate, appointmentHour)).thenReturn(true);
        when(userService.getUser(jwt)).thenReturn(user);
        when(repository.countFutureAppointments(eq(user.getId()), any(), any())).thenReturn(3L);

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.program(newAppointment, jwt), "Se espera que se lance una excepción si el usuario tiene más de dos citas programadas");
        var body = exception.getBody();
        assertEquals(400, body.getStatus(), "El código de estado de la excepción no es el esperado");
    }

    @Test
    void cancel_CuandoLosDatosSonCorrectos_DebeCancelarLaCita() {

        // Arrange
        var appointmentId = 1L;
        var reason = "Razón de la cancelación";

        var cancelAppointment = new CancelAppointment();
        cancelAppointment.setAppointmentId(appointmentId);
        cancelAppointment.setReason(reason);

        var appointment = Appointment.builder()
                .id(appointmentId)
                .attendant(user)
                .professional(user)
                .date(LocalDate.now())
                .hour(10)
                .type(Appointment.Type.PRESENTIAL)
                .build();

        // Mocks
        when(userService.getUser(jwt)).thenReturn(user);
        when(repository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(appointment));

        // Act
        service.cancel(cancelAppointment, jwt);

        // Assert
        assertTrue(appointment.isCanceled(), "La cita debe estar cancelada");
        assertEquals(reason, appointment.getCancellationReason(), "La razón de la cancelación no es la esperada");
    }

    @Test
    void cancel_CuandoUsuarioNoEsElAtendido_DebeLanzarExcepcion() {

        // Arrange
        var appointmentId = 1L;
        var reason = "Razón de la cancelación";

        var cancelAppointment = new CancelAppointment();
        cancelAppointment.setAppointmentId(appointmentId);
        cancelAppointment.setReason(reason);

        var appointment = Appointment.builder()
                .id(appointmentId)
                .attendant(User.builder().id(2L).build())
                .professional(user)
                .date(LocalDate.now())
                .hour(10)
                .type(Appointment.Type.PRESENTIAL)
                .build();

        // Mocks
        when(userService.getUser(jwt)).thenReturn(user);
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(appointment));

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.cancel(cancelAppointment, jwt), "Se espera que se lance una excepción si el usuario no es el atendido");
        var body = exception.getBody();
        assertEquals(403, body.getStatus(), "El código de estado de la excepción no es el esperado");
    }

    @Test
    void cancel_CuandoLaCitaYaFueCancelada_DebeLanzarExcepcion() {

        // Arrange
        var appointmentId = 1L;
        var reason = "Razón de la cancelación";

        var cancelAppointment = new CancelAppointment();
        cancelAppointment.setAppointmentId(appointmentId);
        cancelAppointment.setReason(reason);

        var appointment = Appointment.builder()
                .id(appointmentId)
                .attendant(user)
                .professional(user)
                .date(LocalDate.now())
                .hour(10)
                .type(Appointment.Type.PRESENTIAL)
                .canceled(true)
                .build();

        // Mocks
        when(userService.getUser(jwt)).thenReturn(user);
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(appointment));

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.cancel(cancelAppointment, jwt), "Se espera que se lance una excepción si la cita ya fue cancelada");
        var body = exception.getBody();
        assertEquals(400, body.getStatus(), "El código de estado de la excepción no es el esperado");
    }

    @Test
    void remindAppointments_EnvioDeNotificacionesCorrecto() {
        // Arrange
        var today = LocalDate.now();
        var tomorrow = today.plusDays(1);

        var user1 = User.builder()
                .id(1L)
                .keycloakId("keycloak1")
                .build();

        var user2 = User.builder()
                .id(2L)
                .keycloakId("keycloak2")
                .build();

        var appointments = List.of(
                Appointment.builder().id(1L).date(tomorrow).attendant(user1).build(),
                Appointment.builder().id(2L).date(tomorrow).attendant(user2).build(),
                Appointment.builder().id(3L).date(today).attendant(user1).build()
        );

        when(repository.getAppointmentsByDate(tomorrow)).thenReturn(List.of(appointments.get(0), appointments.get(1)));
        // Act
        service.remindAppoinments();

        // Assert
        verify(repository).getAppointmentsByDate(tomorrow);

        // Verifica que se envíen notificaciones solo a los IDs únicos [1, 2]
        verify(notificationService).send(
                List.of(1L, 2L),
                "Recordatorio de un evento en tu calendario menstrual",
                "Te recordamos que te prepares, ¡no olvides asistir!"
        );
    }
}
