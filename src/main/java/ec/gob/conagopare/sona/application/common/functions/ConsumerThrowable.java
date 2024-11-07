package ec.gob.conagopare.sona.application.common.functions;

import java.util.function.Consumer;

@FunctionalInterface
public interface ConsumerThrowable<T, V extends Throwable> {
    void accept(T t) throws V;

    static <T, V extends Throwable> Consumer<T> unchecked(ConsumerThrowable<T, V> consumer) {
        return t -> {
            try {
                consumer.accept(t);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }
}