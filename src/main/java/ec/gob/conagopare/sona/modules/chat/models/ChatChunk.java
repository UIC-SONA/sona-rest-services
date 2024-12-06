package ec.gob.conagopare.sona.modules.chat.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "chat_chunks")
@CompoundIndex(name = "unique_number_in_room", def = "{'number': 1, 'room.$id': 1}", unique = true)
public class ChatChunk {

    public static final long CHUNK_LENGTH_MB = 5;
    public static final long MAX_CHUNK_SIZE = CHUNK_LENGTH_MB * 1024 * 1024; // 5 MB en bytes

    @Id
    private String id;

    private long number;

    @JsonIgnore
    @DBRef(lazy = true)
    private ChatRoom room;

    private List<ChatMessage> messages = new ArrayList<>();

    public static ChatChunk withFirstMessage(ChatRoom room, long number, ChatMessage message) {
        return ChatChunk.builder()
                .room(room)
                .number(number)
                .messages(List.of(message))
                .build();
    }
}
