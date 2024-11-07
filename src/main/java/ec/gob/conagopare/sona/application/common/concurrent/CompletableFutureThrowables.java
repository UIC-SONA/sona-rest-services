package ec.gob.conagopare.sona.application.common.concurrent;

import ec.gob.conagopare.sona.application.common.functions.RunnableThrowable;
import ec.gob.conagopare.sona.application.common.functions.SupplierThrowable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

public class CompletableFutureThrowables {

    public static <T, V extends Throwable> CompletableFuture<T> supplyAsync(SupplierThrowable<T, V> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Throwable e) {
                throw new CompletionException(e);
            }
        });
    }

    public static <T, V extends Throwable> CompletableFuture<T> supplyAsync(SupplierThrowable<T, V> supplier, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Throwable e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    public static <V extends Throwable> CompletableFuture<Void> runAsync(RunnableThrowable<V> runnable) {
        return CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                throw new CompletionException(e);
            }
        });
    }

    public static <V extends Throwable> CompletableFuture<Void> runAsync(RunnableThrowable<V> runnable, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                throw new CompletionException(e);
            }
        }, executor);
    }
}

