package ec.gob.conagopare.sona.modules.chatbot.repositories;

import ec.gob.conagopare.sona.modules.chatbot.models.PromptResponses;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PromptResponseRepository extends MongoRepository<PromptResponses, String> {
    List<PromptResponses> findAllBySessionId(String session);
}