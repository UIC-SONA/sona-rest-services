package ec.gob.conagopare.sona.application.filters;

import com.google.cloud.logging.Context;
import com.google.cloud.logging.ContextHandler;
import com.google.cloud.logging.HttpRequest;
import com.google.common.base.Strings;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoggingRequestFilter extends HttpFilter {

    private static final String CLOUD_TRACE_CONTEXT_HEADER = "x-cloud-trace-context";
    private static final String W3C_TRACEPARENT_HEADER = "traceparent";
    private final transient ContextHandler contextHandler = new ContextHandler();

    @Override
    public void doFilter(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain chain
    ) throws IOException, ServletException {

        var currentContext = this.contextHandler.getCurrentContext();

        try {
            var logHttpRequest = generateLogEntryHttpRequest(request, response);
            var builder = Context.newBuilder().setRequest(logHttpRequest);
            var tracingHeader = request.getHeader(W3C_TRACEPARENT_HEADER);

            if (tracingHeader != null) {
                builder.loadW3CTraceParentContext(tracingHeader);
            } else {
                builder.loadCloudTraceContext(request.getHeader(CLOUD_TRACE_CONTEXT_HEADER));
            }

            this.contextHandler.setCurrentContext(builder.build());
            super.doFilter(request, response, chain);
        } finally {
            this.contextHandler.setCurrentContext(currentContext);
        }

    }

    private static HttpRequest generateLogEntryHttpRequest(HttpServletRequest req, HttpServletResponse resp) {
        if (req == null) {
            return null;
        }

        var builder = HttpRequest.newBuilder();
        builder
                .setReferer(req.getHeader("referer"))
                .setRemoteIp(req.getRemoteAddr())
                .setRequestMethod(HttpRequest.RequestMethod.valueOf(req.getMethod()))
                .setRequestSize(req.getContentLengthLong())
                .setRequestUrl(composeFullUrl(req))
                .setServerIp(req.getLocalAddr())
                .setUserAgent(req.getHeader("user-agent"));

        if (resp != null) {
            builder
                    .setStatus(resp.getStatus())
                    .setResponseSize(resp.getBufferSize());
        }

        return builder.build();

    }

    private static String composeFullUrl(HttpServletRequest req) {
        var query = req.getQueryString();
        return Strings.isNullOrEmpty(query)
                ? req.getRequestURL().toString()
                : req.getRequestURL().append("?").append(query).toString();
    }

}
