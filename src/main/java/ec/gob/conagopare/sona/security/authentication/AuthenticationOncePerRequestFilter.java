package ec.gob.conagopare.sona.security.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Log4j2
@Component
public class AuthenticationOncePerRequestFilter extends OncePerRequestFilter {

    private final List<AuthenticationRequestResolver<?, ?>> resolvers;

    private final HandlerExceptionResolver exceptionResolver;

    public AuthenticationOncePerRequestFilter(List<AuthenticationRequestResolver<?, ?>> resolvers, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
        for (var resolver : resolvers) {
            log.info("Adding authentication resolver {} to the authentication filter.", resolver.getClass().getSimpleName());
        }
        this.resolvers = resolvers;
    }

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        SecurityContext context = SecurityContextHolder.getContext();

        if (context.getAuthentication() != null) {
            log.debug("Authentication already exists in the security context. Skipping authentication.");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            for (AuthenticationRequestResolver<?, ?> resolver : resolvers) {
                Optional<? extends Authentication> authentication = resolver.resolve(request);
                log.debug("Authentication resolved by {}.", resolver.getClass().getSimpleName());
                if (authentication.isPresent() && authentication.get().isAuthenticated()) {
                    log.debug("Setting authentication in the security context, authentication: {}", authentication.get());
                    context.setAuthentication(authentication.get());
                    break;
                }
            }
        } catch (Exception error) {
            exceptionResolver.resolveException(request, response, null, error);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
