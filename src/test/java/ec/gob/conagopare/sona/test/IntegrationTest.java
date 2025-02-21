package ec.gob.conagopare.sona.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import ec.gob.conagopare.sona.modules.user.UserConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;

import java.io.File;
import java.time.Duration;
import java.util.List;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
public abstract class IntegrationTest {

    private static final String REALM_NAME = "sona";
    private static final String REALM_FILE = "/test-realm.json";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String CLIENT_ID = "sona-core";
    private static final String KC_USER_SYNC_API_KEY = "123456";

    private static final String POSTGRES_DB_NAME = "sona";
    private static final String POSTGRES_USERNAME = "sona";
    private static final String POSTGRES_PASSWORD = "sona";

    @Autowired
    private UserConfig userConfig;

    @Container
    private static final KeycloakContainer KEYCLOAK_CONTAINER = new KeycloakContainer("quay.io/keycloak/keycloak:26.0.2")
            .withReuse(true)
            .withRealmImportFile(REALM_FILE)
            .withAdminUsername(ADMIN_USERNAME)
            .withAdminPassword(ADMIN_PASSWORD)
            .withProviderLibsFrom(List.of(new File("keycloak/spi-usersync/keycloack-spi-user-sync-1.0-jar-with-dependencies.jar")))
            .withExtraHost("host.testcontainers.internal", "host-gateway")
            .withEnv("KC_SPI_EVENTS_LISTENER_USER_SYNC_SYNC_URL", String.format("{\"%s\": \"http://host.testcontainers.internal:8080/user/keycloak-sync\"}", REALM_NAME))
            .withEnv("KC_SPI_EVENTS_LISTENER_USER_SYNC_API_KEY", String.format("{\"%s\": \"%s\"}", REALM_NAME, KC_USER_SYNC_API_KEY));


    @Container
    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:13.3")
            .withReuse(true)
            .withDatabaseName(POSTGRES_DB_NAME)
            .withUsername(POSTGRES_USERNAME)
            .withPassword(POSTGRES_PASSWORD)
            .dependsOn(KEYCLOAK_CONTAINER)
            .waitingFor(Wait.forListeningPort()
                    .withStartupTimeout(Duration.ofSeconds(30)));

    @Container
    private static final MongoDBContainer MONGO_CONTAINER = new MongoDBContainer("mongo:7.0.9")
            .withReuse(true)
            .dependsOn(KEYCLOAK_CONTAINER)
            .waitingFor(Wait.forListeningPort()
                    .withStartupTimeout(Duration.ofSeconds(30)));


    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // Propiedades de Keycloak
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + REALM_NAME);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + REALM_NAME + "/protocol/openid-connect/certs");
        registry.add("keycloak.client-id", () -> CLIENT_ID);
        registry.add("keycloak.cli.server-url", KEYCLOAK_CONTAINER::getAuthServerUrl);
        registry.add("keycloak.cli.realm-master", () -> "master");
        registry.add("keycloak.cli.user", () -> ADMIN_USERNAME);
        registry.add("keycloak.cli.password", () -> ADMIN_PASSWORD);
        registry.add("keycloak.cli.default-client.realm", () -> REALM_NAME);
        registry.add("user.sync-api-key", () -> KC_USER_SYNC_API_KEY);

        // Propiedades de Postgres
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);

        // Propiedades de MongoDB
        registry.add("spring.data.mongodb.uri", MONGO_CONTAINER::getReplicaSetUrl);
    }

    // Mét0do setUp que se ejecuta antes de cada prueba
    @BeforeEach
    public void verifyContainers() {
        // Asegura que los contenedores están corriendo antes de las pruebas
        assert KEYCLOAK_CONTAINER.isRunning() : "Keycloak container is not running";
        assert POSTGRES_CONTAINER.isRunning() : "Postgres container is not running";
        assert MONGO_CONTAINER.isRunning() : "MongoDB container is not running";
    }

    private static final RestTemplate REST_TEMPLATE = new RestTemplate();

    public static String obtainAccessToken(String usernameOrEmail, String password) {

        String tokenUrl = KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + REALM_NAME + "/protocol/openid-connect/token";

        var headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");

        var body = new LinkedMultiValueMap<String, String>();
        body.add("client_id", CLIENT_ID);
        body.add("client_secret", "MY-SECRET");
        body.add("username", usernameOrEmail);
        body.add("password", password);
        body.add("grant_type", "password");

        var request = new HttpEntity<>(body, headers);

        var response = REST_TEMPLATE.exchange(
                tokenUrl,
                HttpMethod.POST,
                request,
                String.class
        );

        try {
            var objectMapper = new ObjectMapper();
            var jsonNode = objectMapper.readTree(response.getBody());
            return jsonNode.get("access_token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Error parsing access token", e);
        }
    }

    public final Credentials getAdminCredentials() {
        var admin = userConfig.getBootstrap().getAdmin();
        return new Credentials(admin.getUsername(), admin.getPassword());
    }

    public record Credentials(String username, String password) {
    }
}
