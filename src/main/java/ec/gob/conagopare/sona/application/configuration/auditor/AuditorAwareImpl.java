package ec.gob.conagopare.sona.application.configuration.auditor;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;
import java.util.Optional;


@Log4j2
public record AuditorAwareImpl() implements AuditorAware<String> {

    @Override
    public @NotNull Optional<String> getCurrentAuditor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (Objects.isNull(authentication)) {
            return Optional.of("System");
        }

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt.getSubject());
        } else {
            return Optional.of(authentication.getName());
        }
    }
}
