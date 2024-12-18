package ec.gob.conagopare.sona.application.common.utils.functions;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Extractor<T, R> {

    @NotNull
    R extract(T t);
}
