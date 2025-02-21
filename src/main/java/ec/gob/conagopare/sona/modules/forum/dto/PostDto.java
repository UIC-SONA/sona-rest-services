package ec.gob.conagopare.sona.modules.forum.dto;

import io.github.luidmidev.jakarta.validations.NoProfanity;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostDto {

    private Boolean anonymous;

    @NotNull(message = "El título de la publicación no puede estar vacío")
    @NotEmpty(message = "El contenido de la publicación no puede estar vacío")
    @Size(min = 5, max = 5000, message = "El contenido de la publicación debe tener entre 5 y 5000 caracteres")
    @NoProfanity
    private String content;
}