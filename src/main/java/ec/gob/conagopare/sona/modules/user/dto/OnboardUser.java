package ec.gob.conagopare.sona.modules.user.dto;


import io.github.luidmidev.jakarta.validations.EquatorCi;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class OnboardUser {

    @NotNull
    @NotEmpty
    @EquatorCi
    protected String ci;

    @NotNull
    protected LocalDate dateOfBirth;
}
