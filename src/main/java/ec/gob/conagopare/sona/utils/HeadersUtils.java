package ec.gob.conagopare.sona.utils;

import org.springframework.http.HttpHeaders;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

public final class HeadersUtils {


    private HeadersUtils() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public static Consumer<HttpHeaders> getHeadersForFile(String filename, boolean inline) {

        var safeFilename = NormalizerUtils.toASCII(filename);
        var encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        var headerValue = (inline ? "inline" : "attachment") + "; filename=\"" + safeFilename + "\"; filename*=UTF-8''" + encodedFilename;
        return headers -> {
            headers.set(CONTENT_DISPOSITION, headerValue);
            headers.setAccessControlExposeHeaders(List.of(CONTENT_DISPOSITION));
        };
    }

    public static Consumer<HttpHeaders> getHeadersForFile(String filename) {
        return getHeadersForFile(filename, false);
    }


}
