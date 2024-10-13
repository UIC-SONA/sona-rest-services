package ec.gob.conagopare.sona.dto;


import com.ketoru.validations.Password;
import com.ketoru.validations.structs.DefaultPasswordRules;
import lombok.Data;

@Data
public class RecoveryPasswordData {

    @Password(DefaultPasswordRules.class)
    private String newPassword;

    private String recoveryToken;
}
