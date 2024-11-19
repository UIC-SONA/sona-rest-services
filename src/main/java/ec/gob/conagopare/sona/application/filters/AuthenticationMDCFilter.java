package ec.gob.conagopare.sona.application.filters;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.MDC;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class AuthenticationMDCFilter extends OncePerRequestFilter {

    private static final String SUBJECT = "subject";
    private static final String TYPE = "type";

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {

        var context = SecurityContextHolder.getContext();
        var authentication = context.getAuthentication();

        if (authentication != null) {
            MDC.put(TYPE, authentication.getClass().getSimpleName());
            if (authentication instanceof JwtAuthenticationToken token) {
                var jwt = token.getToken();
                MDC.put(SUBJECT, jwt.getSubject());
            } else {
                MDC.put(SUBJECT, authentication.getName());
            }
        } else {
            MDC.put(SUBJECT, "<anonymous>");
        }

        super.doFilter(request, response, filterChain);
    }
}
