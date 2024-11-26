package ec.gob.conagopare.sona.application.common.converters.http;

import ec.gob.conagopare.sona.application.common.converters.FromString;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
public class CollectionHttpMessageConverter extends StringGenericHttpMessageConverter<Collection<?>> {

    private static final Map<Class<?>, Function<Stream<?>, Collection<?>>> COLLECTION_PROVIDERS = Map.of(
            List.class, Stream::toList,
            Set.class, stream -> stream.collect(Collectors.toSet()),
            Queue.class, stream -> stream.collect(Collectors.toCollection(ArrayDeque::new)),
            Deque.class, stream -> stream.collect(Collectors.toCollection(LinkedList::new))
    );

    @Override
    public boolean supports(@NotNull Class<?> clazz) {
        return parser(clazz).isPresent();
    }

    @Override
    public boolean canRead(@NotNull Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
        return type instanceof Class<?> clazz ? canRead(clazz, mediaType) : (parser(type).isPresent() && canRead(mediaType));
    }

    private static Optional<FromString<?>> parser(Type type) {
        var parameterizedType = (type instanceof Class<?> clazz
                ? toParameterizedType(clazz.getGenericSuperclass())
                : toParameterizedType(type)).orElse(null);

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
        var mapped = values.stream().map(parser::parse);
        return toCollection(clazz, mapped);
    }

    private static Collection<?> toCollection(Class<?> clazz, Stream<?> mapped) {
        return Optional.ofNullable(COLLECTION_PROVIDERS.get(clazz))
                .map(provider -> provider.apply(mapped))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported collection type: " + clazz + ". Supported types: " + COLLECTION_PROVIDERS.keySet()));
    }

    @Override
    protected String toString(Collection<?> value, Type type) {
        return value.stream().map(Object::toString).collect(Collectors.joining(","));
    }
}