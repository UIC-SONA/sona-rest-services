package ec.gob.conagopare.sona.modules.chatbot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "chatbot")
public class ChaBotConfig {


    private final Session session = new Session();

    @Data
    public static class Session {
        private String project;
        private String location;
        private String agent;
    }


}
