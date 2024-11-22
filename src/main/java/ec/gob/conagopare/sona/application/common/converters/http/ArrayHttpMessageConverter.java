package ec.gob.conagopare.sona.application.common.converters.http;

import ec.gob.conagopare.sona.application.common.converters.FromString;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.Function;

@Slf4j
@Getter
public class ArrayHttpMessageConverter<T> extends StringHttpMessageConverter<Object> {

    private final Class<T> arrayType;
    private final Function<String, T> fromString;

    private ArrayHttpMessageConverter(Class<T> arrayType, Function<String, T> fromString) {
        super();
        this.arrayType = arrayType;
        this.fromString = fromString;
    }

    public boolean supports(Class<?> clazz) {
        return clazz.isArray() && arrayType.isAssignableFrom(clazz.getComponentType());
    }

    @Override
    protected Object fromString(String content, Class<?> clazz) {
        var values = Arrays.asList(content.split(","));
        var list = values.stream().map(fromString).toList();
        var size = list.size();
        var array = Array.newInstance(clazz.getComponentType(), size);
        for (var i = 0; i < size; i++) {
            Array.set(array, i, list.get(i));
        }
        return array;
    }

    @Override
    protected String toString(Object value) {
        var buffer = new StringBuilder();
        var length = Array.getLength(value);
        for (int i = 0; i < length; i++) {
            buffer.append(Array.get(value, i));
            if (i < length - 1) buffer.append(",");
        }
        return buffer.toString();
    }

    public static <T> ArrayHttpMessageConverter<T> of(Class<T> arrayType, Function<String, T> fromString) {
        return new ArrayHttpMessageConverter<>(arrayType, fromString);
    }

    public static <T> ArrayHttpMessageConverter<T> of(FromString<T> stringParser) {
        return of(stringParser.type(), stringParser.parser());
    }
}
