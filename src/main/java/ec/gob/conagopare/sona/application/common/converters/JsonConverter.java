package ec.gob.conagopare.sona.application.common.converters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.SneakyThrows;

import java.util.List;

@Converter
public abstract class JsonConverter<T> implements AttributeConverter<T, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
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

}
