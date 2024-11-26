package ec.gob.conagopare.sona.application.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.TimeZone;


@Slf4j
@Component
public class TimeZoneInterceptor implements HandlerInterceptor {
    private static final ThreadLocal<TimeZone> TIME_ZONE_THREAD_LOCAL = new ThreadLocal<>();
    private static final String TIME_ZONE_HEADER_NAME = "X-Time-Zone";

    /**
     * Sets the time zone for the current thread based on the value of the "timeZoneHeaderName" header in the HttpServletRequest object.
     *
     * @param request The HttpServletRequest object representing the current request.
     * @param response The HttpServletResponse object representing the current response.
     * @param handler The Object representing the handler that is being executed for the current request.
     * @return true if the request should proceed with further processing, false if it should be terminated.
     * @throws Exception if an error occurs while processing the request.
     */ //Tests won't work without explicitly setting the header name
    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
        log.debug("TimeZoneInterceptor:: Inside preHandle");
        String timeZoneHeader = request.getHeader(TIME_ZONE_HEADER_NAME);
        if (timeZoneHeader != null) {
            log.debug("TimeZoneInterceptor:: found header {}", timeZoneHeader);
            TimeZone timeZone = TimeZone.getTimeZone(timeZoneHeader);
            TIME_ZONE_THREAD_LOCAL.set(timeZone);
        } else {
            log.debug("TimeZoneInterceptor:: header not found, setting default time zone");
            TIME_ZONE_THREAD_LOCAL.set(TimeZone.getDefault());
        }
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    /**
     * Performs cleanup after the request has been completed.
     * Removes the time zone from the current thread.
     *
     * @param request The HttpServletRequest object representing the completed request.
     * @param response The HttpServletResponse object representing the completed response.
     * @param handler The Object representing the handler that was executed for the request.
     * @param ex The Exception that occurred during the request processing, or null if no exception occurred.
     * @throws Exception if an error occurs while performing cleanup.
     */
    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, Exception ex) throws Exception {
        log.debug("TimeZoneInterceptor:: Inside afterCompletion");
        TIME_ZONE_THREAD_LOCAL.remove();
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }

    /**
     * Returns the time zone associated with the current thread.
     *
     * @return the time zone associated with the current thread
     */
    public static TimeZone getTimeZone() {
        return TIME_ZONE_THREAD_LOCAL.get();
    }

}
