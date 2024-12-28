package ec.gob.conagopare.sona.application.common.converters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.SneakyThrows;

import java.time.LocalDate;
import java.util.List;

@Converter
public abstract class JsonConverter<T> implements AttributeConverter<T, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    private final TypeReference<T> typeReference;

    protected JsonConverter() {
        this.typeReference = new TypeReference<>() {
        };
    }


    @SneakyThrows
    @Override
    public String convertToDatabaseColumn(T attribute) {
        if (attribute == null) return null;
        return OBJECT_MAPPER.writeValueAsString(attribute);
    }

    @SneakyThrows
    @Override
    public T convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) return null;
        return OBJECT_MAPPER.readValue(dbData, typeReference);
    }

    public static class ListStringConverter extends JsonConverter<List<String>> {
    }

    public static class ListLocalDateConverter extends JsonConverter<List<LocalDate>> {
    }
}
