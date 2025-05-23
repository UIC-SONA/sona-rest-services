package ec.gob.conagopare.sona.application.common.utils;

import org.springframework.http.HttpHeaders;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

public final class HttpHeadersUtils {


    HttpHeadersUtils() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public static HttpHeaders getHeadersForFile(String filename, boolean inline) {

        var safeFilename = NormalizerUtils.toASCII(filename);
        var encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        var headerValue = (inline ? "inline" : "attachment") + "; filename=\"" + safeFilename + "\"; filename*=UTF-8''" + encodedFilename;

        var headers = new HttpHeaders();
        headers.set(CONTENT_DISPOSITION, headerValue);
        headers.setAccessControlExposeHeaders(List.of(CONTENT_DISPOSITION));

        return headers;
    }

    public static HttpHeaders getHeadersForFile(String filename) {
        return getHeadersForFile(filename, false);
    }


}
