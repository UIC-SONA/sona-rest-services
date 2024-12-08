package ec.gob.conagopare.sona.modules.chat.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "chat_rooms")
public class ChatRoom {

    @Id
    private String id;

    private String name;

    private ChatRoomType type;

    @Builder.Default
    private List<Long> participants = new ArrayList<>();
}
