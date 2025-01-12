package ec.gob.conagopare.sona.modules.chat.dto;

import ec.gob.conagopare.sona.modules.chat.models.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ChatMessagePayload {
    private ChatMessage message;
    private String roomId;
    private String requestId;
}
