package ec.gob.conagopare.sona.application.configuration.keycloak;

import java.util.List;
import java.util.Map;

public record KeycloakUserSync(
        String userId,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        String realmId,
        Map<String, String> attributes,
        List<String> realmRoles,
        Map<String, List<String>> clientRoles) {
}