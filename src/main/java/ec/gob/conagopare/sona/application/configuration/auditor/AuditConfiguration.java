package ec.gob.conagopare.sona.application.configuration.auditor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "dateTimeProvider")
public class AuditConfiguration {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();

            if (Objects.isNull(authentication)) {
                return Optional.of("System");
            }

            if (authentication.getPrincipal() instanceof Jwt jwt) {
                return Optional.of(jwt.getSubject());
            } else {
                return Optional.of(authentication.getName());
            }
        };
    }

    @Bean
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(Instant.now());
    }

}