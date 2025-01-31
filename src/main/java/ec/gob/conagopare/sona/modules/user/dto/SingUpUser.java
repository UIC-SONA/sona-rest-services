package ec.gob.conagopare.sona.modules.user.dto;

import io.github.luidmidev.jakarta.validations.Password;
import io.github.luidmidev.jakarta.validations.utils.DefaultPasswordRules;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SingUpUser extends BaseUser {

    @NotNull
    @Password(value = DefaultPasswordRules.class, message = "La contraseña es insegura")
    private String password;

}
