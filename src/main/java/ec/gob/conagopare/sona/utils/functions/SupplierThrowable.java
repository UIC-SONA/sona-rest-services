package ec.gob.conagopare.sona.utils.functions;

@FunctionalInterface
public interface SupplierThrowable<T, V extends Exception> {
    T get() throws V;
}