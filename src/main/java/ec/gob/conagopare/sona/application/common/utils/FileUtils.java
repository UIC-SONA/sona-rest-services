package ec.gob.conagopare.sona.application.common.utils;

import java.time.LocalDateTime;
import java.util.UUID;

public final class FileUtils {

    private FileUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String getExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return (lastDotIndex == -1) ? "" : fileName.substring(lastDotIndex + 1);
    }

    public static String factoryUUIDFileName(String fileName) {
        return factoryUUIDFileName("", fileName);
    }

    public static String factoryUUIDFileName(String prefix, String fileName) {
        return prefix + UUID.randomUUID() + "." + getExtension(fileName);
    }

    public static String factoryDateTimeFileName(String fileName) {
        return factoryDateTimeFileName("", fileName);
    }

    public static String factoryDateTimeFileName(String prefix, String fileName) {
        return prefix + LocalDateTime.now().toString().replace(":", "-") + "." + getExtension(fileName);
    }

}
