package ec.gob.conagopare.sona.modules.user.dto;

import io.github.luidmidev.jakarta.validations.EquatorCi;
import io.github.luidmidev.jakarta.validations.Password;
import io.github.luidmidev.jakarta.validations.structs.DefaultPasswordRules;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.keycloak.representations.idm.UserRepresentation;

@EqualsAndHashCode(callSuper = true)
@Data
public class SignupUser extends UpdateUser {

    @NotNull
    @NotEmpty
    @EquatorCi
    protected String ci;

    @NotNull
    @NotEmpty
    private String username;

    @NotNull
    @Password(DefaultPasswordRules.class)
    private String password;

    @NotNull
    @Email
    private String email;

    public UserRepresentation toUserRepresentation() {
        var userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(username);
        userRepresentation.setFirstName(firstName);
        userRepresentation.setLastName(lastName);
        userRepresentation.setEmail(email);
        return userRepresentation;
    }
}
