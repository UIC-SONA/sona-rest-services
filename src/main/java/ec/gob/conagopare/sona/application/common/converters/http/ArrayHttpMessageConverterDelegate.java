package ec.gob.conagopare.sona.application.common.converters.http;

import ec.gob.conagopare.sona.application.common.converters.FromString;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ArrayHttpMessageConverterDelegate extends StringHttpMessageConverter<Object> {

    private final List<? extends ArrayHttpMessageConverter<?>> converters = FromString.ALL
            .stream()
            .map(ArrayHttpMessageConverter::of)
            .toList();

    private final Map<Class<?>, ArrayHttpMessageConverter<?>> cache = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    private <T> Optional<ArrayHttpMessageConverter<T>> getConverter(Class<?> clazz) {
        return Optional.ofNullable((ArrayHttpMessageConverter<T>) cache.computeIfAbsent(clazz, key ->
                converters.stream()
                        .filter(converter -> converter.supports(clazz))
                        .findFirst()
                        .orElse(null)));
    }

    @Override
    protected Object fromString(String content, Class<?> clazz) {
        return getConverter(clazz)
                .map(converter -> converter.fromString(content, clazz))
                .orElseThrow(() -> new IllegalArgumentException("No se encontró un convertidor para la clase: " + clazz.getComponentType().getName()));
    }

    @Override
    protected String toString(Object value) {
        return getConverter(value.getClass())
                .map(converter -> converter.toString(value))
                .orElseThrow(() -> new IllegalArgumentException("No se encontró un convertidor para la clase: " + value.getClass().getComponentType().getName()));
    }

    @Override
    protected boolean supports(@NotNull Class<?> clazz) {
        return clazz.isArray() && getConverter(clazz).isPresent();
    }
}
