package ec.gob.conagopare.sona.application.common.utils.concurrent;

import ec.gob.conagopare.sona.application.common.utils.functions.RunnableThrowable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

public final class CompletableFutureThrowables {

    private CompletableFutureThrowables() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public static <V extends Exception> CompletableFuture<Void> runAsync(RunnableThrowable<V> runnable) {
        return CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    public static <V extends Exception> CompletableFuture<Void> runAsync(RunnableThrowable<V> runnable, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }
}

