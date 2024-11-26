package ec.gob.conagopare.sona.modules.chatbot.repositories;

import ec.gob.conagopare.sona.modules.chatbot.models.ChatBotSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ChatBotSessionRepository extends MongoRepository<ChatBotSession, String> {
    Optional<ChatBotSession> findBySession(String session);
}
