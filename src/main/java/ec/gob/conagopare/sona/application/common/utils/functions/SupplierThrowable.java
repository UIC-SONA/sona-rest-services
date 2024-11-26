package ec.gob.conagopare.sona.application.common.utils.functions;

import java.util.function.Supplier;

@FunctionalInterface
public interface SupplierThrowable<T, V extends Exception> {
    T get() throws V;

    static <T, V extends Exception> Supplier<T> unchecked(SupplierThrowable<T, V> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}