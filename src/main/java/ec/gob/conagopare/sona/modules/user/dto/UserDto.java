package ec.gob.conagopare.sona.modules.user.dto;

import ec.gob.conagopare.sona.modules.user.models.Authority;
import io.github.luidmidev.jakarta.validations.Password;
import io.github.luidmidev.jakarta.validations.utils.DefaultPasswordRules;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserDto extends BaseUser {

    @NotNull
    private List<Authority> authorityToAdd;

    @NotNull
    private List<Authority> authorityToRemove;

    @Password(DefaultPasswordRules.class)
    private String password;
}