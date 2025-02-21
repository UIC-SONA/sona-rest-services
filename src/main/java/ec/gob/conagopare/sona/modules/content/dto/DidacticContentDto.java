package ec.gob.conagopare.sona.modules.content.dto;


import io.github.luidmidev.jakarta.validations.FileSize;
import io.github.luidmidev.jakarta.validations.Image;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DidacticContentDto {
    @NotNull
    @NotEmpty
    private String title;

    @NotNull
    @NotEmpty
    private String content;

    @Image
    @FileSize(value = 5, unit = FileSize.Unit.MB)
    private MultipartFile image;
}
