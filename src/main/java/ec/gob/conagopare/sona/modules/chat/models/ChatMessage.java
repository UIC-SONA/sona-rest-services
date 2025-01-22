package ec.gob.conagopare.sona.modules.chat.models;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {

    private String id;

    private String message;

    private Instant createdAt;

    private Long sentBy;

    private ChatMessageType type;

    @Field(write = Field.Write.NON_NULL)
    private String resource;

    @Builder.Default
    private List<ReadBy> readBy = new ArrayList<>();

    public static ChatMessage now(String message, Long sentBy, ChatMessageType type) {
        return ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .message(message)
                .createdAt(Instant.now())
                .sentBy(sentBy)
                .type(type)
                .build();
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReadBy {
        private Long participantId;
        private Instant readAt;

        public static ReadBy now(Long participantId) {
            return ReadBy.builder()
                    .participantId(participantId)
                    .readAt(Instant.now())
                    .build();
        }
    }

}