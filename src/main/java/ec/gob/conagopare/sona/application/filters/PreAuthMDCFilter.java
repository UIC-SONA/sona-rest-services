package ec.gob.conagopare.sona.application.filters;


import com.google.cloud.logging.Payload;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.MDC;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class PreAuthMDCFilter extends OncePerRequestFilter {

    private static final String IPADDRESS = "address";
    private static final String HOSTNAME = "hostname";
    private static final String PATH = "path";

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {
        try {
            var address = request.getRemoteAddr();
            var hostname = request.getRemoteHost();

            MDC.put(IPADDRESS, address);
            MDC.put(HOSTNAME, hostname);
            MDC.put(PATH, request.getRequestURI());

            super.doFilter(request, response, filterChain);
        } finally {
            MDC.clear();
        }


    }
}
