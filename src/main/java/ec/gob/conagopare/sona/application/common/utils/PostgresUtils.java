package ec.gob.conagopare.sona.application.common.utils;

import java.time.DayOfWeek;
import java.util.Set;
import java.util.stream.Collectors;

public final class PostgresUtils {
    private PostgresUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static Set<Integer> convertToPostgresOrdinals(Set<DayOfWeek> days) {
        return days.stream()
                .map(day -> (day.getValue() % 7)) // Ajusta 7 (Domingo) a 0 para PostgreSQL
                .collect(Collectors.toSet());
    }
}
