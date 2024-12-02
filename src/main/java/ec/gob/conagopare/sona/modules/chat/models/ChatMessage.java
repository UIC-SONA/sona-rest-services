package ec.gob.conagopare.sona.modules.chat.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "chat_messages")
public class ChatMessage {
    @Id
    private String id;

    @DBRef
    @JsonIgnore
    private ChatRoom chatRoom;

    private String message;

    private LocalDateTime createdAt;

    private Long sentBy;

    private ChatMessageType messageType;

    private String attachment;


    @JsonProperty
    public String getChatRoomId() {
        return chatRoom.getId();
    }
}