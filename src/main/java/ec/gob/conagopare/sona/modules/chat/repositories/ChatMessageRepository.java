package ec.gob.conagopare.sona.modules.chat.repositories;

import ec.gob.conagopare.sona.modules.chat.models.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

}
