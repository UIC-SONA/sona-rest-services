package ec.gob.conagopare.sona.application.configuration.keycloak;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    private String clientId;

    private KeycloakCli cli = new KeycloakCli();

    @Data
    public static class KeycloakCli {
        private String serverUrl;
        private String realmMaster = "master";
        private String adminCli = "admin-cli";
        private String user;
        private String password;

        private KeycloakClient defaultClient = new KeycloakClient();
    }

    @Data
    public static class KeycloakClient {
        private String realm;
//      private UUID clientUiid;
    }
}
