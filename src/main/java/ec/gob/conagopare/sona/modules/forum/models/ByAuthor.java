package ec.gob.conagopare.sona.modules.forum.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public abstract class ByAuthor<T> {

    public static final String AUTHOR_FIELD = "author";
    public static final String ANONYMOUS_FIELD = "anonymous";

    @JsonIgnore
    private T author;

    @JsonIgnore
    private boolean anonymous;

    @JsonProperty("author")
    public T getAuthor() {
        return anonymous ? null : author;
    }

    @JsonIgnore
    public T getRealAuthor() {
        return author;
    }

}
