package ec.gob.conagopare.sona.utils.functions;

@FunctionalInterface
public interface FunctionThrowable<T, R, V extends Throwable> {
    R apply(T t) throws V;
}