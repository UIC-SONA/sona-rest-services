package ec.gob.conagopare.sona.application.common.functions;

import java.util.function.Supplier;

@FunctionalInterface
public interface SupplierThrowable<T, V extends Throwable> {
    T get() throws V;

    static <T, V extends Throwable> Supplier<T> unchecked(SupplierThrowable<T, V> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }
}