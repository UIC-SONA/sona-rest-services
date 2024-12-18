package ec.gob.conagopare.sona.modules.bot.repositories;

import ec.gob.conagopare.sona.modules.bot.models.PromptResponses;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PromptResponseRepository extends MongoRepository<PromptResponses, String> {
    List<PromptResponses> findAllBySession(String session);
}
