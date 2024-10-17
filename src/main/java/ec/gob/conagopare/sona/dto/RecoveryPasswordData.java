package ec.gob.conagopare.sona.dto;


import com.ketoru.validations.Password;
import com.ketoru.validations.structs.DefaultPasswordRules;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecoveryPasswordData {

    @Password(DefaultPasswordRules.class)
    private String newPassword;

    @NotNull
    private String recoveryCode;

}
