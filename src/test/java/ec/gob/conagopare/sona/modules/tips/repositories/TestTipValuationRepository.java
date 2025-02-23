package ec.gob.conagopare.sona.modules.tips.repositories;

import static org.junit.jupiter.api.Assertions.*;


import ec.gob.conagopare.sona.modules.tips.models.TipRate;
import ec.gob.conagopare.sona.modules.tips.models.Tip;
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

import java.time.LocalDateTime;
import java.util.List;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true"
})
@Transactional
class TestTipValuationRepository {

    @Autowired
    private TipValuationRepository tipValuationRepository;

    @Autowired
    private TipRepository tipRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private Tip tip;

    @BeforeEach
    void setUp() {
        // Crear un usuario de prueba
        user = userRepository.save(User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@test.com")
                .username("johndoe")
                .enabled(true)
                .keycloakId("keycloak123")
                .build());

        // Crear un Tip de prueba
        tip = tipRepository.save(Tip.builder()
                .title("Tip de prueba")
                .summary("Resumen del tip")
                .description("Descripción del tip")
                .tags(List.of("tag1", "tag2"))
                .image("path/to/image")
                .active(true)
                .build());

        // Guardar una calificación para el usuario y el tip
        tipValuationRepository.save(TipRate.builder()
                .tip(tip)
                .user(user)
                .value(5)
                .valuationDate(LocalDateTime.now())
                .build());
    }

    @AfterEach
    void tearDown() {
        tipValuationRepository.deleteAll();
        tipRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void fingUserValuation_CuandoExisteCalificacion_DebeRetornarCalificacionDelUsuario() {
        var result = tipValuationRepository.fingUserValuation(tip.getId(), user.getId());

        assertNotNull(result, "Se espera que se retorne la calificación del usuario");
        assertEquals(5, result, "La calificación no es la esperada");
    }

    @Test
    void fingUserValuation_CuandoNoExisteCalificacion_DebeRetornarNull() {
        var anotherUser = userRepository.save(User.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@test.com")
                .username("janedoe")
                .enabled(true)
                .keycloakId("keycloak456")
                .build());

        var result = tipValuationRepository.fingUserValuation(tip.getId(), anotherUser.getId());

        assertNull(result, "Se espera que no exista calificación para el otro usuario");
    }

    @Test
    void findByTipIdAndUserId_CuandoExisteCalificacion_DebeRetornarTipRate() {
        var result = tipValuationRepository.findByTipIdAndUserId(tip.getId(), user.getId());

        assertTrue(result.isPresent(), "Se espera que se retorne el TipRate");
        assertEquals(tip.getId(), result.get().getTip().getId(), "El ID del Tip no es el esperado");
        assertEquals(user.getId(), result.get().getUser().getId(), "El ID del usuario no es el esperado");
    }

    @Test
    void findByTipIdAndUserId_CuandoNoExisteCalificacion_DebeRetornarEmpty() {
        var anotherUser = userRepository.save(User.builder()
                .firstName("Alice")
                .lastName("Smith")
                .email("alice.smith@test.com")
                .username("alicesmith")
                .enabled(true)
                .keycloakId("keycloak789")
                .build());

        var result = tipValuationRepository.findByTipIdAndUserId(tip.getId(), anotherUser.getId());

        assertTrue(result.isEmpty(), "Se espera que no exista calificación para el otro usuario");
    }

    @Test
    void findValuations_CuandoExistenCalificaciones_DebeRetornarListaDeCalificaciones() {
        var result = tipValuationRepository.findValuations(tip.getId());

        assertNotNull(result, "Se espera que se retorne una lista de calificaciones");
        assertFalse(result.isEmpty(), "Se espera que la lista de calificaciones no esté vacía");
        assertTrue(result.contains(5), "La calificación 5 no está en la lista de calificaciones");
    }

    @Test
    void findValuations_CuandoNoExistenCalificaciones_DebeRetornarListaVacia() {
        var newTip = tipRepository.save(Tip.builder()
                .title("Nuevo Tip")
                .summary("Resumen del nuevo tip")
                .description("Descripción del nuevo tip")
                .tags(List.of("tag3"))
                .image("path/to/newimage")
                .active(true)
                .build());

        var result = tipValuationRepository.findValuations(newTip.getId());

        assertNotNull(result, "Se espera que se retorne una lista de calificaciones vacía");
        assertTrue(result.isEmpty(), "Se espera que la lista de calificaciones esté vacía");
    }
}
