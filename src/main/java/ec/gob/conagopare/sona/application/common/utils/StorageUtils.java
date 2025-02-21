package ec.gob.conagopare.sona.application.common.utils;

import io.github.luidmidev.storage.PurgableStored;
import io.github.luidmidev.storage.Storage;
import io.github.luidmidev.storage.ToStore;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;

import java.util.List;
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

    public static void tryRemoveFileAsync(Storage storage, @NotNull List<String> fullPaths) {
        for (var fullPath : fullPaths) {
            runAsync(() -> storage.remove(fullPath)).exceptionally(canNotRemoveFile(fullPath));
        }
    }

    public static void purgeAsync(Storage storage, @NotNull PurgableStored purgableStored) {
        runAsync(() -> storage.purge(purgableStored)).exceptionally(throwable -> {
            MDC.put("reporter", "StorageUtils");
            log.error("Cannot purge files from storage: {}", purgableStored.filesFullPaths(), throwable);
            return null;
        });
    }


}
