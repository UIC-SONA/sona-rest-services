package ec.gob.conagopare.sona.modules.user.dto;

import ec.gob.conagopare.sona.application.common.validations.SonaPassword;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserDto extends BaseUser {

    @NotNull
    private Set<Authority> authoritiesToAdd;

    @NotNull
    private Set<Authority> authoritiesToRemove;

    @SonaPassword
    private String password;
}
