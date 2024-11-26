package ec.gob.conagopare.sona.application.common.utils.functions;

public interface RunnableThrowable<V extends Exception> {
    void run() throws V;

    static <V extends Exception> RunnableThrowable<V> unchecked(RunnableThrowable<V> runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
