package ec.gob.conagopare.sona.utils.functions;

@FunctionalInterface
public interface SupplierThrowable<T, V extends Throwable> {
    T get() throws V;
}