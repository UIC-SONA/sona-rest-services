package ec.gob.conagopare.sona.modules.chat.repositories;

import ec.gob.conagopare.sona.modules.chat.models.ChatRoom;
import ec.gob.conagopare.sona.modules.chat.models.ChatRoomType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {

    /**
     * Buscar todas las salas que contengan un participante específico.
     */
    @Query(value = "{ 'participants': ?0 }")
    List<ChatRoom> findByParticipant(Long participantId);

    /**
     * Buscar la sala que contenga a los participantes específicos.
     */
    @Query(value = "{ 'participants': { $all: ?0 }, 'type': ?1 }")
    Optional<ChatRoom> findByParticipantsAndType(List<Long> senderId, ChatRoomType chatRoomType);
}