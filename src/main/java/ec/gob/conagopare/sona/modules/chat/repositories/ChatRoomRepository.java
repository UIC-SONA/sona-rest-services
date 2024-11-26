package ec.gob.conagopare.sona.modules.chat.repositories;

import ec.gob.conagopare.sona.modules.chat.models.ChatRoom;
import ec.gob.conagopare.sona.modules.chat.models.ChatRoomType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {

    Optional<ChatRoom> findByParticipantsAndType(List<Long> participants, ChatRoomType type);

    List<ChatRoom> findAllByParticipantsContaining(List<Long> participants);
}
