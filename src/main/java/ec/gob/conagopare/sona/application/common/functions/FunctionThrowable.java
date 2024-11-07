package ec.gob.conagopare.sona.application.common.functions;

import java.util.function.Function;

@FunctionalInterface
public interface FunctionThrowable<T, R, V extends Throwable> {
    R apply(T t) throws V;

    static <T, R, V extends Throwable> Function<T, R> unchecked(FunctionThrowable<T, R, V> function) {
        return t -> {
            try {
                return function.apply(t);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }
}