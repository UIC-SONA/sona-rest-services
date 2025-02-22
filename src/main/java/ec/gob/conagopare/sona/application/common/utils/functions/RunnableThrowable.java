package ec.gob.conagopare.sona.application.common.utils.functions;

public interface RunnableThrowable<V extends Exception> {
    void run() throws V;
}
