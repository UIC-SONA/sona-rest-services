package ec.gob.conagopare.sona.modules.chat.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class PrivateMessageSent {
    /**
     * For private chat, this is the target user id
     */
    @NotNull
    private Long recipientId;

    /**
     * Message content
     */
    @NotEmpty
    private String content;
}