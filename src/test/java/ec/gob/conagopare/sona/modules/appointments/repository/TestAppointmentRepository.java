package ec.gob.conagopare.sona.modules.appointments.repository;

import ec.gob.conagopare.sona.modules.appointments.models.Appointment;
import ec.gob.conagopare.sona.modules.appointments.models.ProfessionalSchedule;
import ec.gob.conagopare.sona.modules.user.models.Authority;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true"
})
@Transactional
class TestAppointmentRepository {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ProfessionalScheduleRepository scheduleRepository;

    @Autowired
    private UserRepository userRepository;

    private User professional;
    private User attendant;
    private Appointment appointment;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();

        professional = userRepository.save(User.builder()
                .firstName("Professional")
                .lastName("Test")
                .email("professional@test.com")
                .authorities(Set.of(Authority.MEDICAL_PROFESSIONAL))
                .username("professional")
                .enabled(true)
                .keycloakId("123")
                .build());

        attendant = userRepository.save(User.builder()
                .firstName("Attendant")
                .lastName("Test")
                .email("attendant@test.com")
                .authorities(Set.of(Authority.USER))
                .username("attendant")
                .enabled(true)
                .keycloakId("456")
                .build());

        scheduleRepository.save(ProfessionalSchedule.builder()
                .professional(professional)
                .date(today)
                .fromHour(9)
                .toHour(17)
                .build());

        appointment = appointmentRepository.save(Appointment.builder()
                .professional(professional)
                .attendant(attendant)
                .date(today)
                .hour(10)
                .type(Appointment.Type.VIRTUAL)
                .canceled(false)
                .build());
    }

    @AfterEach
    void tearDown() {
        appointmentRepository.deleteAll();
        scheduleRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void existsAppointmentAtHour_CuandoExisteCita_DebeRetornarTrue() {
        var exists = appointmentRepository.existsAppointmentAtHour(
                professional.getId(),
                today,
                10
        );
        assertTrue(exists);
    }

    @Test
    void existsAppointmentAtHour_CuandoNoExisteCita_DebeRetornarFalse() {
        var exists = appointmentRepository.existsAppointmentAtHour(
                professional.getId(),
                today,
                11
        );
        assertFalse(exists);
    }

    @Test
    void existsAppointmentAtHour_CuandoCitaEstaCancelada_DebeRetornarFalse() {
        appointment.setCanceled(true);
        appointmentRepository.save(appointment);

        var exists = appointmentRepository.existsAppointmentAtHour(
                professional.getId(),
                today,
                10
        );
        assertFalse(exists);
    }

    @Test
    void isWithinProfessionalSchedule_CuandoHoraDentroHorario_DebeRetornarTrue() {
        var isWithin = appointmentRepository.isWithinProfessionalSchedule(
                professional.getId(),
                today,
                10
        );
        assertTrue(isWithin);
    }

    @Test
    void isWithinProfessionalSchedule_CuandoHoraFueraHorario_DebeRetornarFalse() {
        var isWithin = appointmentRepository.isWithinProfessionalSchedule(
                professional.getId(),
                today,
                8
        );
        assertFalse(isWithin);
    }

    @Test
    void getProfessionalAppointmentsRanges_CuandoHayCitas_DebeRetornarRangosCorrectos() {
        var from = today.minusDays(1);
        var to = today.plusDays(1);

        var ranges = appointmentRepository.getProfessionalAppointmentsRanges(
                professional.getId(),
                from, to
        );

        assertFalse(ranges.isEmpty());
        assertEquals(1, ranges.size());
        var range = ranges.getFirst();

        assertEquals(today.atTime(10, 0), range.getFrom());
        assertEquals(today.atTime(11, 0), range.getTo());
    }

    @Test
    void getAppointmentsByDate_CuandoExistenCitas_DebeRetornarOrdenadasPorHora() {
        appointmentRepository.save(Appointment.builder()
                .professional(professional)
                .attendant(attendant)
                .date(today)
                .hour(14)
                .type(Appointment.Type.PRESENTIAL)
                .canceled(false)
                .build());

        var appointments = appointmentRepository.getAppointmentsByDate(today);

        assertEquals(2, appointments.size()); // Se espera 2 citas
        assertTrue(appointments.get(0).getHour() < appointments.get(1).getHour());
    }

    @Test
    void countFutureAppointments_CuandoHayCitasFuturas_DebeRetornarConteoCorrecto() {
        appointmentRepository.save(Appointment.builder()
                .professional(professional)
                .attendant(attendant)
                .date(today.plusDays(1))
                .hour(10)
                .type(Appointment.Type.VIRTUAL)
                .canceled(false)
                .build());

        var count = appointmentRepository.countFutureAppointments(
                attendant.getId(), today, 9);

        assertEquals(2, count);
    }

}
