package ec.gob.conagopare.sona.application.common.converters.http;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public abstract class StringHttpMessageConverter<T> extends AbstractHttpMessageConverter<T> {

    private static final MediaType[] SUPPORTED_MEDIA_TYPES = {
            MediaType.APPLICATION_OCTET_STREAM,
            MediaType.TEXT_PLAIN
    };

    protected StringHttpMessageConverter() {
        super(StandardCharsets.UTF_8, SUPPORTED_MEDIA_TYPES);
    }


    @Override
    public @NotNull T readInternal(@NotNull Class<? extends T> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        var bytes = inputMessage.getBody().readAllBytes();
        var content = new String(bytes, Objects.requireNonNull(getDefaultCharset()));
        return fromString(content, clazz);
    }


    @Override
    public void writeInternal(@NotNull T value, @NotNull HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        var content = toString(value);
        outputMessage.getBody().write(content.getBytes(Objects.requireNonNull(getDefaultCharset())));
    }

    protected abstract T fromString(String content, Class<? extends T> clazz);

    protected abstract String toString(T value);


}
