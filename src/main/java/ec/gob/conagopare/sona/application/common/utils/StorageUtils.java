package ec.gob.conagopare.sona.application.common.utils;

import io.github.luidmidev.storage.Storage;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.function.Function;

import static ec.gob.conagopare.sona.application.common.utils.concurrent.CompletableFutureThrowables.runAsync;

@Slf4j
public final class StorageUtils {

    private StorageUtils() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    private static Function<Throwable, Void> canNotRemoveFile(String fullPath) {
        return throwable -> {
            MDC.put("reporter", "StorageUtils");
            log.error("Cannot remove file from storage: {}", fullPath, throwable);
            return null;
        };
    }

    public static void tryRemoveFileAsync(Storage storage, String... fullPaths) {
        for (var fullPath : fullPaths) {
            if (fullPath == null) continue;
            runAsync(() -> storage.remove(fullPath)).exceptionally(canNotRemoveFile(fullPath));
        }
    }
}
