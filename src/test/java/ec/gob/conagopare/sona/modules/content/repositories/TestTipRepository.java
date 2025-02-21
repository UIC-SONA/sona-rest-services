package ec.gob.conagopare.sona.modules.content.repositories;

import ec.gob.conagopare.sona.modules.content.models.Tip;
import ec.gob.conagopare.sona.modules.content.models.TipRate;
import ec.gob.conagopare.sona.modules.user.models.User;
import ec.gob.conagopare.sona.modules.user.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true"
})
@Transactional
class TestTipRepository {

    @Autowired
    private TipRepository tipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TipValuationRepository tipValuationRepository;

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
    }

    @AfterEach
    void tearDown() {
        tipValuationRepository.deleteAll();
        tipRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void findByIdMapWithRates_CuandoExisteTip_DebeRetornarMapConDatos() {
        var tipId = tip.getId();  // Obtener el UUID del Tip creado en setUp()
        var userId = user.getId(); // Obtener el ID del usuario

        tipValuationRepository.save(TipRate.builder()
                .tip(tip)
                .user(user)
                .value(5)
                .valuationDate(LocalDateTime.now())
                .build());

        var result = tipRepository.findByIdMapWithRates(tipId, userId);

        assertTrue(result.isPresent(), "Se espera que se retorne un mapa con los datos del Tip");
        var tipData = result.get();
        assertEquals(tip.getId(), ((Tip) tipData.get("tip")).getId(), "El ID del Tip no es el esperado");
        assertNotNull(tipData.get("myRate"), "El valor de mi calificación no debe ser nulo");
        assertNotNull(tipData.get("averageRate"), "El valor de la calificación promedio no debe ser nulo");
        assertNotNull(tipData.get("totalRate"), "El total de calificaciones no debe ser nulo");
    }

    @Test
    void findAllMapWithRatings_CuandoHayVariosTips_DebeRetornarPageConMap() {
        var userId = user.getId();  // Obtener el ID del usuario
        var pageable = PageRequest.of(0, 10);

        var page = tipRepository.findAllMapWithRatings(userId, true, pageable);

        assertNotNull(page, "El resultado de la página no debe ser nulo");
        assertFalse(page.isEmpty(), "Se espera que la página contenga al menos un Tip");
        var tipData = page.getContent().getFirst();
        assertNotNull(tipData.get("tip"), "El mapa debe contener un 'tip'");
        assertNotNull(tipData.get("averageRate"), "La calificación promedio no debe ser nula");
    }

    @Test
    void searchAllMapWithRatings_CuandoSeBuscaPorTag_DebeRetornarPageConResultados() {
        var userId = user.getId();  // Obtener el ID del usuario
        var pageable = PageRequest.of(0, 10);
        var searchTerm = "tag1";

        var page = tipRepository.searchAllMapWithRatings(searchTerm, userId, true, pageable);

        assertNotNull(page, "El resultado de la búsqueda no debe ser nulo");
        assertFalse(page.isEmpty(), "Se espera que la búsqueda retorne resultados");
        var tipData = page.getContent().getFirst();
        assertNotNull(tipData.get("tip"), "El mapa debe contener un 'tip'");
    }

    @Test
    void topRating_CuandoExistenVariosTipsConCalificaciones_DebeRetornarLosTipsConMejorCalificacion() {
        var limit = 5;

        var result = tipRepository.findTopRating(limit);

        assertNotNull(result, "El resultado no debe ser nulo");
        assertFalse(result.isEmpty(), "Se espera que se retornen algunos Tips");
        assertTrue(result.size() <= limit, "El número de Tips debe ser igual o menor que el límite");
        assertNotNull(result.getFirst().get("tip"), "El mapa debe contener un 'tip'");
    }

    @Test
    void getImagePathById_CuandoExisteTipConImagen_DebeRetornarImagen() {
        var tipId = tip.getId();  // Obtener el UUID del Tip creado en setUp()

        var result = tipRepository.getImagePathById(tipId);

        assertTrue(result.isPresent(), "Se espera que se retorne la imagen");
        assertEquals(tip.getImage(), result.get(), "La ruta de la imagen no es la esperada");
    }
}
