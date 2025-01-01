package ec.gob.conagopare.sona.modules.user;

import ec.gob.conagopare.sona.modules.user.dto.SingUpUser;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "user")
public class UserConfig {

    private final Bootstrap bootstrap = new Bootstrap();
    private String syncApiKey;

    @Data
    public static class Bootstrap {
        private boolean enabled = false;
        private BootstrapUser admin;
    }

    @Data
    public static class BootstrapUser {

        private Long id;
        private String firstname;
        private String lastname;
        private String username;
        private String email;
        private String password;

        public SingUpUser toSingUpUser() {
            var info = new SingUpUser();
            info.setFirstName(firstname);
            info.setLastName(lastname);
            info.setUsername(username);
            info.setEmail(email);
            info.setPassword(password);
            return info;
        }
    }
}
