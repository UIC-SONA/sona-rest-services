package ec.gob.conagopare.sona.application.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;


public final class HttpServletUtils {

    private HttpServletUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String extractBearerToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.replace("Bearer ", "");
        }
        return null;
    }

}
