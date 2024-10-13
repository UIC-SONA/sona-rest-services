package ec.gob.conagopare.sona.utils.functions;

@FunctionalInterface
public interface ConsumerThrowable<T, V extends Exception> {
    void accept(T t) throws V;
}