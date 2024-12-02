package ec.gob.conagopare.sona.application.common.utils.functions;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class NoOpt {

    private NoOpt() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    private static final Runnable RUNNABLE = () -> {
    };

    private static final Consumer<?> CONSUMER = t -> {
    };

    private static final BiConsumer<?, ?> BICONSUMER = (t, u) -> {
    };

    public static Runnable runnable() {
        return RUNNABLE;
    }

    @SuppressWarnings("unchecked")
    public static <T> Consumer<T> consumer() {
        return (Consumer<T>) CONSUMER;
    }

    @SuppressWarnings("unchecked")
    public static <T, U> BiConsumer<T, U> biConsumer() {
        return (BiConsumer<T, U>) BICONSUMER;
    }
}
