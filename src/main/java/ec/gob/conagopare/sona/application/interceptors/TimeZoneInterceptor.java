package ec.gob.conagopare.sona.application.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.ZoneId;
import java.util.TimeZone;


@Slf4j
public class TimeZoneInterceptor implements HandlerInterceptor {
    private static final ThreadLocal<TimeZone> TIME_ZONE_THREAD_LOCAL = new ThreadLocal<>();
    private static final String TIME_ZONE_HEADER_NAME = "X-Time-Zone";


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

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, Exception ex) throws Exception {
        log.debug("TimeZoneInterceptor:: Inside afterCompletion");
        TIME_ZONE_THREAD_LOCAL.remove();
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }

    public static TimeZone getTimeZone() {
        return TIME_ZONE_THREAD_LOCAL.get();
    }


    public static ZoneId getZoneId() {
        return getTimeZone().toZoneId();
    }

}
