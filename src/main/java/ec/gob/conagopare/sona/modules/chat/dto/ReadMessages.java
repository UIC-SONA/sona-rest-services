package ec.gob.conagopare.sona.modules.chat.dto;

import ec.gob.conagopare.sona.modules.chat.models.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class ReadMessages {
    private String roomId;
    private ChatMessage.ReadBy readBy;
    @Builder.Default
    private List<String> messageIds = new ArrayList<>();
}
