package ec.gob.conagopare.sona.modules.user.dto;

import ec.gob.conagopare.sona.application.common.validations.SonaPassword;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SingUpUser extends BaseUser {

    @NotNull
    @SonaPassword
    private String password;

}
