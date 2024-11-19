package ec.gob.conagopare.sona.modules.user.dto;

import io.github.luidmidev.jakarta.validations.Password;
import io.github.luidmidev.jakarta.validations.utils.DefaultPasswordRules;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.keycloak.representations.idm.UserRepresentation;

@Data
public class SingUpUser {

    @NotNull
    @NotEmpty
    private String firstName;

    @NotNull
    @NotEmpty
    private String lastName;

    @NotNull
    @NotEmpty
    private String username;

    @NotNull
    @Email
    private String email;

    @NotNull
    @Password(DefaultPasswordRules.class)
    private String password;


    public UserRepresentation toUserRepresentation() {
        var representation = new UserRepresentation();
        representation.setFirstName(firstName);
        representation.setLastName(lastName);
        representation.setUsername(username);
        representation.setEmail(email);
        return representation;
    }
}
