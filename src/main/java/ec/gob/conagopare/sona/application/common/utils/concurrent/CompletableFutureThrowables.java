package ec.gob.conagopare.sona.application.common.concurrent;

import ec.gob.conagopare.sona.application.common.functions.RunnableThrowable;
import ec.gob.conagopare.sona.application.common.functions.SupplierThrowable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

public final class CompletableFutureThrowables {

    private CompletableFutureThrowables() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public static <T, V extends Exception> CompletableFuture<T> supplyAsync(SupplierThrowable<T, V> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    public static <T, V extends Exception> CompletableFuture<T> supplyAsync(SupplierThrowable<T, V> supplier, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
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

