package ec.gob.conagopare.sona.modules.user.repositories;

import static org.junit.jupiter.api.Assertions.*;


import ec.gob.conagopare.sona.modules.user.models.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true"
})
@Transactional
class UserRepositoryUnitTest {

    @Autowired
    private UserRepository userRepository;


    @BeforeEach
    void setUp() {
        userRepository.save(User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@test.com")
                .username("johndoe")
                .enabled(true)
                .keycloakId("keycloak123")
                .build());
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void findByKeycloakId_CuandoExisteUsuario_DebeRetornarUsuario() {
        Optional<User> foundUser = userRepository.findByKeycloakId("keycloak123");

        assertTrue(foundUser.isPresent(), "Se espera que el usuario sea encontrado por su keycloakId");
        assertEquals("john.doe@test.com", foundUser.get().getEmail(), "El email del usuario no coincide");
    }

    @Test
    void findByKeycloakId_CuandoNoExisteUsuario_DebeRetornarEmpty() {
        Optional<User> foundUser = userRepository.findByKeycloakId("keycloak999");

        assertFalse(foundUser.isPresent(), "No se espera que el usuario sea encontrado por un keycloakId inexistente");
    }
}
