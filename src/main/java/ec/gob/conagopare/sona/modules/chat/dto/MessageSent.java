package ec.gob.conagopare.sona.modules.chat.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class MessageSent {

    @NotNull
    private String chatRoomId;
    @NotNull
    @NotEmpty
    private String content;
}