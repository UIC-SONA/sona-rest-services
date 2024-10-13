package ec.gob.conagopare.sona.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public final class HttpServletUtils {

    private HttpServletUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static final ObjectMapper mapper = new ObjectMapper();

    public static String extractBearerToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.replace("Bearer ", "");
        }
        return null;
    }


    public static String extractFullSubPath(HttpServletRequest request, String parentPath) {
        var uri = request.getRequestURI();
        var path = uri.substring(request.getContextPath().length() + parentPath.length());
        int queryParamIndex = path.indexOf("?");
        if (queryParamIndex != -1) {
            path = path.substring(0, queryParamIndex);
        }
        return URLDecoder.decode(path, StandardCharsets.UTF_8);
    }

//    public static void writeError(int code, String message, HttpServletResponse response) throws IOException {
//        if (!isErrorStatus(code)) throw new IllegalArgumentException("Invalid error code");
//        String json = ErrorResponse.jsonOf(message, code);
//        write(json, response, code);
//    }

    public static void writeResponse(int code, Object body, HttpServletResponse response) throws IOException {
        if (body instanceof String str) {
            write(str, response, code);
            return;
        }

        String json = mapper.writeValueAsString(body);
        write(json, response, code);
    }


    public static void addRedirectHeader(HttpServletResponse response, String location) {
        response.addHeader("Location", location);
        response.setStatus(HttpServletResponse.SC_FOUND);
    }


    private static boolean isErrorStatus(int status) {
        return status >= 400;
    }

    private static void write(String json, HttpServletResponse response, int status) throws IOException {
        response.setContentType("application/json");
        response.setStatus(status);
        response.getWriter().write(json);
    }


    public static void writeHeader(String name, String value, HttpServletResponse response) {
        response.addHeader(name, value);
    }
}
