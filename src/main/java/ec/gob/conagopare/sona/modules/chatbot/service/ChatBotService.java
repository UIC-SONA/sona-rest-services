package ec.gob.conagopare.sona.modules.chatbot.service;

import ec.gob.conagopare.sona.modules.chatbot.models.ChatBotSession;
import ec.gob.conagopare.sona.modules.chatbot.models.PromptResponse;
import ec.gob.conagopare.sona.modules.chatbot.repositories.ChatBotSessionRepository;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Validated
@RequiredArgsConstructor
public abstract class ChatBotService {

    private final ChatBotSessionRepository sessionRepository;

    /**
     * MEtodo abstracto para realizar la lógica interna de chat.
     */
    protected abstract List<String> internalChat(String session, String prompt);

    /**
     * MEtodo principal para manejar un mensaje de chat.
     */
    public PromptResponse sendMessage(@NotNull @NotEmpty String session, String prompt) {
        var response = internalChat(session, prompt);
        var chatBotSession = getOrCreateSession(session);

        return savePromptResponse(chatBotSession, prompt, response);
    }

    /**
     * Obtiene una sesión existente o crea una nueva si no existe.
     */
    public ChatBotSession getOrCreateSession(String session) {
        return findSession(session).orElseGet(() -> createNewSession(session));
    }

    /**
     * Crea una nueva sesión y la guarda en el repositorio.
     */
    private ChatBotSession createNewSession(String session) {
        return sessionRepository.save(ChatBotSession.builder()
                .session(session)
                .build());
    }

    /**
     * Guarda una respuesta de un prompt en la sesión y retorna su DTO.
     */
    public PromptResponse savePromptResponse(ChatBotSession session, String prompt, List<String> responses) {
        var promptResponse = PromptResponse.builder()
                .prompt(prompt)
                .responses(responses)
                .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        session.getPromptResponses().add(promptResponse);
        sessionRepository.save(session);

        return promptResponse;
    }

    /**
     * Convierte la lista de respuestas a su representación en DTO.
     */
    public List<PromptResponse> getChatHistory(String session) {
        return findSession(session)
                .map(ChatBotSession::getPromptResponses)
                .orElse(List.of());
    }


    /**
     * Busca una sesión en el repositorio por su identificador.
     */
    private Optional<ChatBotSession> findSession(String session) {
        return sessionRepository.findBySession(session);
    }
}
