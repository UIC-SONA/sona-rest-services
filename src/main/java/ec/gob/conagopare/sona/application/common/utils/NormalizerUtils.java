package ec.gob.conagopare.sona.application.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.text.Normalizer;

@Slf4j
public final class NormalizerUtils {


    private NormalizerUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static @NotNull String toASCII(@NotNull String string) {
        try {
            var normalizedString = Normalizer.normalize(string, Normalizer.Form.NFD);
            return normalizedString
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                    .replaceAll("[^\\p{ASCII}]", "");
        } catch (Exception e) {
            throw new IllegalArgumentException("Error normalizando la cadena", e);
        }
    }

}
