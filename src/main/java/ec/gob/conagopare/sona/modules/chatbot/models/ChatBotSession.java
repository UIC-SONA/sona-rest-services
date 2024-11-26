package ec.gob.conagopare.sona.modules.chatbot.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "chat_bot_sessions")
public class ChatBotSession {
    @Id
    private String id;

    @Indexed(unique = true)
    private String session;

    @Builder.Default
    @DBRef
    private List<PromptResponse> promptResponses = new ArrayList<>();
}