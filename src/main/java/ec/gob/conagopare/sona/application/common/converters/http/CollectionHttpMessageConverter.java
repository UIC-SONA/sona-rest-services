package ec.gob.conagopare.sona.application.common.converters.http;

import ec.gob.conagopare.sona.application.common.converters.FromString;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toCollection;

@Slf4j
public class CollectionHttpMessageConverter extends StringGenericHttpMessageConverter<Collection<?>> {

    @Override
    public boolean supports(@NotNull Class<?> clazz) {
        return parser(clazz).isPresent();
    }

    @Override
    public boolean canRead(@NotNull Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
        return type instanceof Class<?> clazz ? canRead(clazz, mediaType) : (parser(type).isPresent() && canRead(mediaType));
    }

    private static Optional<FromString<?>> parser(Type type) {
        var parameterizedType = toParameterizedType(type).orElseGet(() -> type instanceof Class<?> clazz ? toParameterizedType(clazz.getGenericSuperclass()).orElse(null) : null);
        if (parameterizedType == null) {
            return Optional.empty();
        }

        if (!Collection.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())) {
            return Optional.empty();
        }

        var elementType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
        return Optional.ofNullable(FromString.from(elementType));
    }

    private static Optional<ParameterizedType> toParameterizedType(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            return Optional.of(parameterizedType);
        }
        return Optional.empty();
    }


    @Override
    protected Collection<?> fromString(String content, Type type) {

        var values = Arrays.asList(content.split(","));
        var parser = parser(type).orElseThrow();
        var clazz = type instanceof Class<?> aClazz ? aClazz : (Class<?>) toParameterizedType(type).orElseThrow().getRawType();

        return values.stream().map(parser::parse).collect(toCollection(() -> {
            if (List.class.isAssignableFrom(clazz)) {
                return new ArrayList<>();
            }
            if (Set.class.isAssignableFrom(clazz)) {
                return new HashSet<>();
            }
            if (Queue.class.isAssignableFrom(clazz)) {
                return new ArrayDeque<>();
            }
            throw new IllegalArgumentException("Unsupported collection type: " + clazz);
        }));
    }

    @Override
    protected String toString(Collection<?> value, Type type) {
        return value.stream().map(Object::toString).collect(Collectors.joining(","));
    }
}