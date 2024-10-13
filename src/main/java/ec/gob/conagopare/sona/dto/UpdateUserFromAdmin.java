package ec.gob.conagopare.sona.dto;


import com.ketoru.validations.Password;
import com.ketoru.validations.structs.DefaultPasswordRules;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserFromAdmin {

    private Boolean enabled;

    private Boolean locked;

    private List<String> authorities;

    @Password(DefaultPasswordRules.class)
    private String password;
}
