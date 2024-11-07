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
public class PostAuthMDCFilter extends OncePerRequestFilter {

    private static final String IPADDRESS = "address";
    private static final String SUBJECT = "subject";
    private static final String PATH = "path";

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {
        var context = SecurityContextHolder.getContext();
        var authentication = context.getAuthentication();

        if (authentication instanceof JwtAuthenticationToken token) {
            var jwt = token.getToken();
            MDC.put(SUBJECT, jwt.getSubject());
        } else if (authentication == null) {
            MDC.put(SUBJECT, "anonymous");
        } else {
            MDC.put(SUBJECT, authentication.getName());
        }

        var address = request.getRemoteAddr();

        MDC.put(IPADDRESS, address);
        MDC.put(PATH, request.getRequestURI());

        super.doFilter(request, response, filterChain);
    }
}
