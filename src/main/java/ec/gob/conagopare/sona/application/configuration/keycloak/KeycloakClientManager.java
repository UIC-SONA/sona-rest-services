package ec.gob.conagopare.sona.application.configuration.keycloak;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;

/**
 * Clase para administrar un cliente de Keycloak
 */
@Slf4j
public class KeycloakClientManager {

    @Getter
    private final String realm;

    @Getter
    private final String clientUiid;

    private final Keycloak keycloak;


    public KeycloakClientManager(Keycloak keycloak, KeycloakProperties properties) {

        log.info("Creating KeycloakClientManager for realm: {} and client: {}", properties.getCli().getDefaultClient().getRealm(), properties.getClientId());

        this.realm = properties.getCli().getDefaultClient().getRealm();
        var realmResource = keycloak.realm(realm)
                .clients()
                .findByClientId(properties.getClientId())
                .getFirst();

        this.clientUiid = realmResource.getId();
        this.keycloak = keycloak;

        log.info("KeycloakClientManager created for realm: {} and client: {}, with clientUiid: {}", realm, properties.getClientId(), clientUiid);
    }

    public RealmResource realm() {
        return keycloak.realm(realm);
    }

    public UsersResource users() {
        return realm().users();
    }

    public ClientResource client() {
        return realm().clients().get(clientUiid);
    }

    public RolesResource roles() {
        return client().roles();
    }
}
