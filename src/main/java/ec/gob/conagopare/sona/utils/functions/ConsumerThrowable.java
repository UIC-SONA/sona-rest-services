package ec.gob.conagopare.sona.utils.functions;

@FunctionalInterface
public interface ConsumerThrowable<T, V extends Throwable> {
    void accept(T t) throws V;
}