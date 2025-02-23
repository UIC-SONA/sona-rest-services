package ec.gob.conagopare.sona.modules.forum.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class NewComment {

    private Boolean anonymous;

    @NotEmpty(message = "El contenido del comentario no puede estar vacío")
    private String content;
}
