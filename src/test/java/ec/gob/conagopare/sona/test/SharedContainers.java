package ec.gob.conagopare.sona.test;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.time.Duration;
import java.util.List;

// SharedContainers.java
public class SharedContainers {

    private static final String REALM_NAME = "sona";
    private static final String REALM_FILE = "/test-realm.json";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String CLIENT_ID = "sona-core";
    private static final String KC_USER_SYNC_API_KEY = "123456";

    private static final String POSTGRES_DB_NAME = "sona";
    private static final String POSTGRES_USERNAME = "sona";
    private static final String POSTGRES_PASSWORD = "sona";

    public static final KeycloakContainer KEYCLOAK_CONTAINER;
    public static final PostgreSQLContainer<?> POSTGRES_CONTAINER;
    public static final MongoDBContainer MONGO_CONTAINER;

    static {
        KEYCLOAK_CONTAINER = new KeycloakContainer("quay.io/keycloak/keycloak:26.0.2")
                .withRealmImportFile(REALM_FILE)
                .withAdminUsername(ADMIN_USERNAME)
                .withAdminPassword(ADMIN_PASSWORD)
                .withProviderLibsFrom(List.of(new File("keycloak/spi-usersync/keycloack-spi-user-sync-1.0-jar-with-dependencies.jar")))
                .withExtraHost("host.testcontainers.internal", "host-gateway")
                .withEnv("KC_SPI_EVENTS_LISTENER_USER_SYNC_SYNC_URL", String.format("{\"%s\": \"http://host.testcontainers.internal:8080/user/keycloak-sync\"}", REALM_NAME))
                .withEnv("KC_SPI_EVENTS_LISTENER_USER_SYNC_API_KEY", String.format("{\"%s\": \"%s\"}", REALM_NAME, KC_USER_SYNC_API_KEY));

        POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:13.3")
                .withDatabaseName(POSTGRES_DB_NAME)
                .withUsername(POSTGRES_USERNAME)
                .withPassword(POSTGRES_PASSWORD)
                .waitingFor(Wait.forListeningPort()
                        .withStartupTimeout(Duration.ofSeconds(30)));

        MONGO_CONTAINER = new MongoDBContainer("mongo:7.0.9")
                .waitingFor(Wait.forListeningPort()
                        .withStartupTimeout(Duration.ofSeconds(30)));

        // Iniciamos los contenedores en orden
        KEYCLOAK_CONTAINER.start();
        POSTGRES_CONTAINER.start();
        MONGO_CONTAINER.start();
    }

    public static String getRealmName() {
        return REALM_NAME;
    }

    public static String getClientId() {
        return CLIENT_ID;
    }

    public static String getAdminUsername() {
        return ADMIN_USERNAME;
    }

    public static String getAdminPassword() {
        return ADMIN_PASSWORD;
    }

    public static String getKcUserSyncApiKey() {
        return KC_USER_SYNC_API_KEY;
    }
}