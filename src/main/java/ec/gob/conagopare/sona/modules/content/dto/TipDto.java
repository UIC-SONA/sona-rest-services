package ec.gob.conagopare.sona.modules.content.dto;

import io.github.luidmidev.jakarta.validations.Image;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.AllArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@AllArgsConstructor
public class TipDto {

    @NotNull
    @NotEmpty
    private String title;

    @NotNull
    @NotEmpty
    private String summary;

    @NotNull
    @NotEmpty
    private String description;

    @NotNull
    @NotEmpty
    private List<String> tags;

    @NotNull
    private boolean active;

    @Image
    private MultipartFile image;
}