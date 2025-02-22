package ec.gob.conagopare.sona.application.common.utils;

import io.github.luidmidev.storage.Stored;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public final class ResponseEntityUtils {

    ResponseEntityUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static ResponseEntity<ByteArrayResource> resource(Stored stored, boolean inline) {
        var info = stored.getInfo();
        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType(info.getContentType()))
                .headers(HttpHeadersUtils.getHeadersForFile(info.getFilename(), inline))
                .body(new ByteArrayResource(stored.getContent()));
    }

    public static ResponseEntity<ByteArrayResource> resource(Stored stored) {
        return resource(stored, false);
    }
}
