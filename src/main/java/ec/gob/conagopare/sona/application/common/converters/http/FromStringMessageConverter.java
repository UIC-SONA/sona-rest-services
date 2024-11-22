package ec.gob.conagopare.sona.application.common.converters.http;

import ec.gob.conagopare.sona.application.common.converters.FromString;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class FromStringMessageConverter extends StringHttpMessageConverter<Object> {

    @Override
    protected boolean supports(@NotNull Class<?> clazz) {
        return FromString.isSupported(clazz);
    }

    @Override
    protected Object fromString(String content, Class<?> clazz) {
        return FromString.from(clazz).parse(content);
    }

    @Override
    protected String toString(Object value) {
        return value.toString();
    }
}
