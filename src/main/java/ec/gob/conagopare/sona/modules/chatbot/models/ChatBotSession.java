package ec.gob.conagopare.sona.modules.chatbot.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "chat_bot_sessions")
public class ChatBotRoom {
    @Id
    private String id;

    private String session;

    @DBRef
    private List<PromptResponse> promptResponses;
}