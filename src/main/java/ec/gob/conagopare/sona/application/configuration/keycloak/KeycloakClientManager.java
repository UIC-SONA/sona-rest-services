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


    public KeycloakClientManager(Keycloak keycloak, KeycloakProperties.KeycloakClient client) {
        this.realm = client.getRealm();
        this.clientUiid = client.getClientUiid().toString();
        this.keycloak = keycloak;
    }

    public RealmResource realm() {
        return keycloak.realm(realm);
    }

    public UsersResource users() {
        return realm().users();
    }

    public ClientResource clients() {
        return realm().clients().get(clientUiid);
    }

    public RolesResource roles() {
        return clients().roles();
    }
}
