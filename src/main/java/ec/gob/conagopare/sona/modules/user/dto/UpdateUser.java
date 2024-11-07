package ec.gob.conagopare.sona.modules.user.dto;


import io.github.luidmidev.jakarta.validations.EquatorCi;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateUser {

    @NotNull
    @NotEmpty
    @EquatorCi
    protected String ci;

    @NotNull
    protected LocalDate dateOfBirth;

    @NotNull
    @NotEmpty
    protected String firstName;

    @NotNull
    @NotEmpty
    protected String lastName;
}
