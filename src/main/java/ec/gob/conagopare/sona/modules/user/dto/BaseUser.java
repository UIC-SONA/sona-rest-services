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
        var representation = new UserRepresentation();
        return transferToRepresentation(representation);
    }

    public UserRepresentation transferToRepresentation(UserRepresentation representation) {
        representation.setFirstName(firstName);
        representation.setLastName(lastName);
        representation.setUsername(username);
        representation.setEmail(email);
        return representation;
    }

}
