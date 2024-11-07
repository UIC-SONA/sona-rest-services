package ec.gob.conagopare.sona.application.common.functions;

public interface RunnableThrowable<V extends Throwable> {
    void run() throws V;

    static <V extends Throwable> RunnableThrowable<V> unchecked(RunnableThrowable<V> runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }
}
