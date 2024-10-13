package ec.gob.conagopare.sona.utils.stats;


import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public interface OrderedPair<T> {
    T x();

    Number y();

    static <T> @NotNull Optional<Number> f(@NotNull List<? extends OrderedPair<T>> stats, T x) {
        return stats.stream().filter(stat -> stat.x().equals(x)).map(OrderedPair::y).findFirst();
    }

    static <T> List<T> x(@NotNull List<? extends OrderedPair<T>> stats) {
        return stats.stream().map(OrderedPair::x).toList();
    }

    static <T> List<Number> y(@NotNull List<? extends OrderedPair<T>> stats) {
        return stats.stream().map(OrderedPair::y).toList();
    }


}