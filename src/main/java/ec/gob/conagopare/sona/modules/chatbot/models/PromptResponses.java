package ec.gob.conagopare.sona.modules.chatbot.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "prompt_responses")
public class PromptResponses {

    private String prompt;

    private List<String> responses;

    private LocalDateTime timestamp;

    @JsonIgnore
    @DBRef(lazy = true)
    private ChatBotSession session;
}
