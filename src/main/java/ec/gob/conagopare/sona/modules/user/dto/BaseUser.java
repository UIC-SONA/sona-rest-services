package ec.gob.conagopare.sona.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.keycloak.representations.idm.UserRepresentation;

@Data
public class BaseUser {

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

    public UserRepresentation toRepresentation() {
        return transferToRepresentation(new UserRepresentation());
    }

    public UserRepresentation transferToRepresentation(UserRepresentation representation) {
        representation.setFirstName(firstName.trim());
        representation.setLastName(lastName.trim());
        representation.setUsername(username.trim());
        representation.setEmail(email.trim());
        return representation;
    }

}
