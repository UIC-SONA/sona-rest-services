package ec.gob.conagopare.sona.dto;



import com.ketoru.validations.Password;
import com.ketoru.validations.structs.DefaultPasswordRules;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUser {

    @Size(min = 3, max = 100)
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚüÜ ]*$")
    private String name;

    @Size(min = 3, max = 100)
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚüÜ ]*$")
    private String lastname;


    @Size(min = 3, max = 100)
    private String username;

    @Password(DefaultPasswordRules.class)
    private String password;
}

