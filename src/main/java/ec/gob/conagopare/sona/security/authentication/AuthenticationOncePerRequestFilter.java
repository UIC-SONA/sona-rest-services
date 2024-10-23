package ec.gob.conagopare.sona.security.authentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

@Log4j2
@Component
public class AuthenticationOncePerRequestFilter extends OncePerRequestFilter {

    private final List<AuthenticationRequestProvider<?>> resolvers;
    private final HandlerExceptionResolver exceptionResolver;

    public AuthenticationOncePerRequestFilter(List<AuthenticationRequestProvider<?>> resolvers, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
        this.resolvers = resolvers;
        for (var resolver : resolvers) {
            log.info("Adding authentication resolver {} to the authentication filter.", resolver.getClass().getSimpleName());
        }
    }

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {

        var context = SecurityContextHolder.getContext();

        if (context.getAuthentication() != null) {
            log.debug("Authentication already exists in the security context. Skipping authentication.");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            for (var resolver : resolvers) {
                var result = resolver.resolve(request);
                log.debug("Authentication resolved by {}.", resolver.getClass().getSimpleName());
                if (result.success()) {
                    var authentication = result.authentication();
                    log.debug("Setting authentication in the security context, authentication: {}", authentication);
                    context.setAuthentication(authentication);
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
