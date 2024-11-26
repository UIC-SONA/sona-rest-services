package ec.gob.conagopare.sona.application.common.utils.functions;

import java.util.function.Consumer;

@FunctionalInterface
public interface ConsumerThrowable<T, V extends Exception> {
    void accept(T t) throws V;

    static <T, V extends Exception> Consumer<T> unchecked(ConsumerThrowable<T, V> consumer) {
        return t -> {
            try {
                consumer.accept(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}