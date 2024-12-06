package ec.gob.conagopare.sona.modules.chat.models;

import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {

    private UUID id;

    private String message;

    private ZonedDateTime createdAt;

    private Long sentBy;

    private ChatMessageType messageType;

}