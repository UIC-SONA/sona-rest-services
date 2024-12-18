package ec.gob.conagopare.sona.modules.forum.dto;

import io.github.luidmidev.jakarta.validations.Image;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@AllArgsConstructor
public class PostDto {

    private Boolean anonymous;

    @NotEmpty(message = "El contenido de la publicación no puede estar vacío")
    private String content;

    @Size(max = 5, message = "No se pueden subir más de 5 imágenes")
    private List<@Image MultipartFile> images;
}
