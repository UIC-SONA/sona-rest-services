package ec.gob.conagopare.sona.application.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String CHAT_ENDPOINT = "/chat";

    @Bean
    public WebSocketMessageBrokerConfigurer chatWebSocketMessageBrokerConfigurer() {
        return new WebSocketMessageBrokerConfigurer() {
            @Override
            public void registerStompEndpoints(@NotNull StompEndpointRegistry registry) {
                registry.addEndpoint(CHAT_ENDPOINT).setAllowedOrigins("*");
                registry.addEndpoint(CHAT_ENDPOINT).setAllowedOrigins("*").withSockJS();
            }

            @Override
            public void configureMessageBroker(@NotNull MessageBrokerRegistry registry) {
                registry.enableSimpleBroker(CHAT_ENDPOINT + "/topic");
                registry.setApplicationDestinationPrefixes(CHAT_ENDPOINT);
            }
        };
    }
}
