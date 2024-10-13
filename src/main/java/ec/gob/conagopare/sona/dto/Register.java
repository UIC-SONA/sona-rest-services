package ec.gob.conagopare.sona.dto;


import com.ketoru.validations.Password;
import com.ketoru.validations.structs.DefaultPasswordRules;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class Register {

    @Size(min = 3, max = 100)
    @NotBlank
    @NotNull
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚüÜ ]*$")
    private String name;

    @Size(min = 3, max = 100)
    @NotBlank
    @NotNull
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚüÜ ]*$")
    private String lastname;


    @Size(min = 3, max = 100)
    @NotBlank
    @NotNull
    private String username;

    @NotNull
    @Password(DefaultPasswordRules.class)
    private String password;

    @Size(min = 3, max = 100)
    @NotBlank
    @NotNull
    @Email
    private String email;

}

