package ec.gob.conagopare.sona.application.common.utils.functions;

import java.util.function.Function;

@FunctionalInterface
public interface FunctionThrowable<T, R, V extends Exception> {
    R apply(T t) throws V;

    @SuppressWarnings("java:S112") // Suppressing this warning because we are rethrowing the exception
    static <T, R, V extends Exception> Function<T, R> unchecked(FunctionThrowable<T, R, V> function) {
        return t -> {
            try {
                return function.apply(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}