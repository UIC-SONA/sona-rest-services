package ec.gob.conagopare.sona.application.configuration.keycloak;

import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clase para administrar un cliente de Keycloak
 */
@Slf4j
@Configuration
public class KeycloakConfiguration {

    @Bean
    public Keycloak keycloak(KeycloakProperties properties) {
        var cli = properties.getCli();
        return KeycloakBuilder.builder()
                .serverUrl(cli.getServerUrl())
                .realm(cli.getRealmMaster())
                .clientId(cli.getAdminCli())
                .username(cli.getUser())
                .password(cli.getPassword())
                .resteasyClient(new ResteasyClientBuilderImpl().connectionPoolSize(15).build())
                .build();

    }

    @Bean
    public KeycloakClientManager keycloakClientManager(
            Keycloak keycloak,
            KeycloakProperties properties
    ) {
        return new KeycloakClientManager(keycloak, properties);
    }
}
