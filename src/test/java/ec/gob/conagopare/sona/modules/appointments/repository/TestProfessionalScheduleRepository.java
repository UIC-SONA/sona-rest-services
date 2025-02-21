package ec.gob.conagopare.sona.modules.appointments.repository;

import ec.gob.conagopare.sona.modules.appointments.models.Appointment;
import ec.gob.conagopare.sona.modules.appointments.models.ProfessionalSchedule;
import ec.gob.conagopare.sona.modules.user.models.User;
import ec.gob.conagopare.sona.modules.user.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true"
})
@Transactional
class TestProfessionalScheduleRepository {

    @Autowired
    private ProfessionalScheduleRepository repository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    private User professional;
    private ProfessionalSchedule schedule;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();

        professional = userRepository.save(User.builder()
                .firstName("Professional")
                .lastName("Test")
                .email("professional@test.com")
                .username("professional")
                .enabled(true)
                .keycloakId("123")
                .build());

        schedule = repository.save(ProfessionalSchedule.builder()
                .professional(professional)
                .date(today)
                .fromHour(9)
                .toHour(17)
                .build());
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void existsOverlappingSchedule_CuandoExisteSuperposicion_DebeRetornarTrue() {
        var exists = repository.existsOverlappingSchedule(
                professional.getId(),
                today,
                10,
                12
        );
        assertTrue(exists, "Se espera que haya una superposición de horario");
    }

    @Test
    void existsOverlappingSchedule_CuandoNoExisteSuperposicion_DebeRetornarFalse() {
        var exists = repository.existsOverlappingSchedule(
                professional.getId(),
                today,
                17,
                18
        );
        assertFalse(exists, "No se espera superposición de horario");
    }

    @Test
    void existsOverlappingScheduleExcludingId_CuandoExisteSuperposicionConExclusion_DebeRetornarTrue() {
        // Crear un horario adicional que se solape con 10-12 (pero no será excluido)
        repository.save(ProfessionalSchedule.builder()
                .professional(professional)
                .date(today)
                .fromHour(11)  // Este horario se solapa con 10-12
                .toHour(13)
                .build());

        // Ahora, verifica si existe un solapamiento excluyendo el horario original
        var exists = repository.existsOverlappingScheduleExcludingId(
                professional.getId(),
                today,
                10,
                12,
                schedule.getId()  // Excluir el horario actual
        );

        assertTrue(exists, "Se espera que haya una superposición de horario con exclusión");
    }


    @Test
    void existsOverlappingScheduleExcludingId_CuandoNoExisteSuperposicionConExclusion_DebeRetornarFalse() {
        var exists = repository.existsOverlappingScheduleExcludingId(
                professional.getId(),
                today,
                17,
                18,
                schedule.getId()
        );
        assertFalse(exists, "No se espera superposición de horario con exclusión");
    }

    @Test
    void existsActiveAppointmentsInSchedule_CuandoExistenCitasActivas_DebeRetornarTrue() {
        // Primero guardamos una cita activa
        var attendant = userRepository.save(User.builder()
                .firstName("Attendant")
                .lastName("Test")
                .email("attendant@test.com")
                .username("attendant")
                .enabled(true)
                .keycloakId("124")
                .build());

        appointmentRepository.save(Appointment.builder()
                .professional(professional)
                .attendant(attendant)
                .date(today)
                .hour(10)
                .type(Appointment.Type.VIRTUAL)
                .canceled(false)
                .build());

        var exists = repository.existsActiveAppointmentsInSchedule(
                professional.getId(),
                today,
                9,
                12
        );
        assertTrue(exists, "Se espera que existan citas activas en el horario");
    }


    @Test
    void existsActiveAppointmentsInSchedule_CuandoNoExistenCitasActivas_DebeRetornarFalse() {
        var exists = repository.existsActiveAppointmentsInSchedule(
                professional.getId(),
                today,
                14,
                16
        );
        assertFalse(exists, "No se esperan citas activas en el horario");
    }

    @Test
    void getSchedulesByProfessional_CuandoExistenHorarios_DebeRetornarHorariosCorrectos() {
        var from = today.minusDays(1);
        var to = today.plusDays(1);

        var schedules = repository.getSchedulesByProfessional(
                professional.getId(),
                from,
                to
        );

        assertFalse(schedules.isEmpty(), "Se esperan horarios para el profesional");
        assertEquals(1, schedules.size(), "Se espera un solo horario para el profesional");
        assertEquals(today, schedules.getFirst().getDate(), "La fecha del horario no es la esperada");
    }

    @Test
    void getSchedulesByProfessional_CuandoNoExistenHorarios_DebeRetornarListaVacia() {
        var from = today.plusDays(1);
        var to = today.plusDays(2);

        var schedules = repository.getSchedulesByProfessional(
                professional.getId(),
                from,
                to
        );

        assertTrue(schedules.isEmpty(), "No se esperan horarios para el profesional en el rango dado");
    }
}
