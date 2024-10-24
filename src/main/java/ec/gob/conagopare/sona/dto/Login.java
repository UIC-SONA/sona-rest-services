package ec.gob.conagopare.sona.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Login {

    @NotNull
    private String username;

    @NotNull
    private String password;

}
