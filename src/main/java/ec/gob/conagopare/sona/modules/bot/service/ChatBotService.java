package ec.gob.conagopare.sona.modules.bot.service;

import ec.gob.conagopare.sona.modules.bot.models.PromptResponses;
import ec.gob.conagopare.sona.modules.bot.repositories.PromptResponseRepository;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Validated
@RequiredArgsConstructor
public abstract class ChatBotService {

    private final PromptResponseRepository promptResponseRepository;

    /**
     * MEtodo abstracto para realizar la lógica interna de chat.
     */
    protected abstract List<String> internalChat(String session, String prompt);

    /**
     * MEtodo principal para manejar un mensaje de chat.
     */
    public PromptResponses sendMessage(@NotNull @NotEmpty String session, String prompt) {
        var response = internalChat(session, prompt);

        return savePromptResponse(session, prompt, response);
    }

    /**
     * Guarda una respuesta de un prompt en la sesión y retorna su DTO.
     */
    public PromptResponses savePromptResponse(String session, String prompt, List<String> responses) {
        var promptResponse = PromptResponses.builder()
                .session(session)
                .prompt(prompt)
                .responses(responses)
                .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        return promptResponseRepository.save(promptResponse);
    }

    /**
     * Convierte la lista de respuestas a su representación en DTO.
     */
    public List<PromptResponses> getChatHistory(String session) {
        return promptResponseRepository.findAllBySession(session);
    }
}
