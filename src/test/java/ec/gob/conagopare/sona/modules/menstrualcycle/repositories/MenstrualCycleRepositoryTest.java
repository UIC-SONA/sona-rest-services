package ec.gob.conagopare.sona.modules.menstrualcycle.repositories;

import ec.gob.conagopare.sona.modules.menstrualcycle.models.CycleData;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true"
})
@Transactional
class MenstrualCycleRepositoryTest {

    @Autowired
    private MenstrualCycleRepository menstrualCycleRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@test.com")
                .username("janedoe")
                .enabled(true)
                .keycloakId("keycloak456")
                .build());

        menstrualCycleRepository.save(CycleData.builder()
                .user(user)
                .periodDuration(5)  // Duración del periodo
                .cycleLength(28)    // Duración del ciclo
                .periodDates(List.of(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 3, 1))) // Fechas de periodo
                .build());
    }

    @AfterEach
    void tearDown() {
        menstrualCycleRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void findByUser_CuandoExisteCicloMenstrual_DebeRetornarCiclo() {
        var foundCycleData = menstrualCycleRepository.findByUser(user);

        assertTrue(foundCycleData.isPresent(), "Se espera que se encuentre el ciclo menstrual del usuario");
        assertEquals(5, foundCycleData.get().getPeriodDuration(), "La duración del periodo no es la esperada");
        assertEquals(28, foundCycleData.get().getCycleLength(), "La duración del ciclo no es la esperada");
        assertTrue(foundCycleData.get().getPeriodDates().contains(LocalDate.of(2025, 2, 1)), "La fecha del periodo no es la esperada");
    }

    @Test
    void findByUser_CuandoNoExisteCicloMenstrual_DebeRetornarEmpty() {
        // Crear otro usuario que no tiene ciclo menstrual
        var anotherUser = userRepository.save(User.builder()
                .firstName("Alice")
                .lastName("Smith")
                .email("alice.smith@test.com")
                .username("alicesmith")
                .enabled(true)
                .keycloakId("keycloak789")
                .build());

        var foundCycleData = menstrualCycleRepository.findByUser(anotherUser);

        assertFalse(foundCycleData.isPresent(), "No se espera que el ciclo menstrual sea encontrado para un usuario sin ciclo");
    }
}
