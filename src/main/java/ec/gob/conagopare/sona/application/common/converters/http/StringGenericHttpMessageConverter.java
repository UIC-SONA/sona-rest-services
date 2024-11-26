package ec.gob.conagopare.sona.application.common.converters.http;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public abstract class StringGenericHttpMessageConverter<T> extends AbstractGenericHttpMessageConverter<T> {

    private static final MediaType[] SUPPORTED_MEDIA_TYPES = {
            MediaType.APPLICATION_OCTET_STREAM,
            MediaType.TEXT_PLAIN
    };

    protected StringGenericHttpMessageConverter() {
        super(SUPPORTED_MEDIA_TYPES);
        super.setDefaultCharset(StandardCharsets.UTF_8);
    }


    @Override
    public @NotNull T read(@NotNull Type type, Class<?> contextClass, @NotNull HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        var bytes = inputMessage.getBody().readAllBytes();
        try {
            var content = new String(bytes, Objects.requireNonNull(getDefaultCharset()));
            return fromString(content, type);
        } catch (Exception e) {
            throw new HttpMessageNotReadableException("Failed to read HTTP message", e, inputMessage);
        }
    }


    @Override
    public @NotNull T readInternal(@NotNull Class<? extends T> clazz, @NotNull HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return read(clazz, clazz, inputMessage);
    }

    @Override
    protected void writeInternal(@NotNull T value, Type type, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        try {
            var content = toString(value, type);
            outputMessage.getBody().write(content.getBytes(Objects.requireNonNull(getDefaultCharset())));
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new HttpMessageNotWritableException("Failed to write HTTP message", e);
        }
    }

    protected abstract T fromString(String content, Type type);

    protected abstract String toString(T value, Type type);


}
