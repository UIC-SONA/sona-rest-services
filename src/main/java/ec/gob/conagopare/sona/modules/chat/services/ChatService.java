package ec.gob.conagopare.sona.modules.chat.services;

import ec.gob.conagopare.sona.modules.chat.dto.ChatMessageSent;
import ec.gob.conagopare.sona.modules.chat.models.ChatChunk;
import ec.gob.conagopare.sona.modules.chat.models.ChatMessage;
import ec.gob.conagopare.sona.modules.chat.models.ChatRoom;
import ec.gob.conagopare.sona.modules.chat.models.ChatRoomType;
import ec.gob.conagopare.sona.modules.chat.repositories.ChatRoomRepository;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static java.util.concurrent.CompletableFuture.runAsync;

@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class ChatService {


    private static final String CHAT_CHUNK_NUMBER_KEY = "number";

    private final SimpMessagingTemplate messaging;
    private final UserService userService;
    private final MongoTemplate mongoTemplate;
    private final ChatRoomRepository roomRepository;

    public ChatMessageSent send(@NotEmpty String message, String roomId, String requestId, Jwt jwt) {
        var user = userService.getUser(jwt);
        var chatRoom = room(roomId);

        if (!chatRoom.getParticipants().contains(user.getId())) {
            throw ApiError.forbidden("No tienes permiso para enviar mensajes");
        }

        var chatMessage = ChatMessage.builder()
                .id(UUID.randomUUID())
                .sentBy(user.getId())
                .message(message)
                .createdAt(ZonedDateTime.now(ZoneId.of("UTC")))
                .build();

        addMessage(chatRoom, chatMessage);

        var chatMessageSent = ChatMessageSent.builder()
                .requestId(requestId)
                .roomId(roomId)
                .message(chatMessage)
                .build();

        runAsync(() -> messaging.convertAndSend("/chat.room." + roomId, chatMessageSent)).exceptionally(logExpecionally("Error enviando mensaje a la sala de chat"));
        for (var participant : chatRoom.getParticipants()) {
            runAsync(() -> messaging.convertAndSend("/chat.inbox." + participant, chatMessageSent)).exceptionally(logExpecionally("Error enviando mensaje a la bandeja de entrada"));
        }

        return chatMessageSent;
    }

    public ChatRoom room(String chatRoomId) {
        return roomRepository.findById(chatRoomId).orElseThrow(() -> ApiError.notFound("No se encontró la sala de chat con el id"));
    }

    public List<ChatRoom> rooms(Jwt jwt) {
        var user = userService.getUser(jwt);
        assert user.getId() != null;
        return roomRepository.findByParticipant(user.getId());
    }

    public ChatRoom room(Long userId, Jwt jwt) {
        var user = userService.getUser(jwt);
        var recipient = userService.getUser(userId);
        return findOrCreatePrivateRoom(user.getId(), recipient.getId());
    }

    private ChatRoom findOrCreatePrivateRoom(Long senderId, Long recipientId) {
        return roomRepository
                .findByParticipantsAndType(List.of(senderId, recipientId), ChatRoomType.PRIVATE)
                .orElseGet(() -> createPrivateRoom(senderId, recipientId));
    }

    private ChatRoom createPrivateRoom(Long senderId, Long recipientId) {
        var newRoom = ChatRoom.builder()
                .type(ChatRoomType.PRIVATE)
                .participants(List.of(senderId, recipientId))
                .name("Private Chat between " + senderId + " and " + recipientId)
                .build();

        return roomRepository.save(newRoom);
    }

    public void addMessage(ChatRoom chatRoom, ChatMessage message) {

        var query = new Query();
        query.addCriteria(Criteria.where("room.$id").is(new ObjectId(chatRoom.getId())));
        query.with(Sort.by(Sort.Order.desc(CHAT_CHUNK_NUMBER_KEY)));

        var projectedQuery = Query.of(query);
        projectedQuery.fields().include("_id").include(CHAT_CHUNK_NUMBER_KEY);

        var latestChunk = mongoTemplate.findOne(projectedQuery, ChatChunk.class);

        if (latestChunk == null) {
            mongoTemplate.save(ChatChunk.withFirstMessage(chatRoom, 1, message));
            return;
        }

        var aproxSize = getDocumentSize(latestChunk.getId(), ChatChunk.class);

        if (aproxSize > ChatChunk.MAX_CHUNK_SIZE) {
            mongoTemplate.save(ChatChunk.withFirstMessage(chatRoom, latestChunk.getNumber() + 1, message));
            return;
        }

        var update = new Update();
        update.push("messages", message);

        mongoTemplate.updateFirst(query, update, ChatChunk.class);


    }

    public long getDocumentSize(String id, Class<?> collectionType) {
        var aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("_id").is(new ObjectId(id))),
                Aggregation.project()
                        .andExclude("_id")
                        .andExpression("bsonSize($$ROOT)").as("size")
        );

        var results = mongoTemplate.aggregate(aggregation, mongoTemplate.getCollectionName(collectionType), Document.class);

        if (!results.getMappedResults().isEmpty()) {
            return results.getMappedResults().getFirst().getLong("size");
        }

        return 0;
    }

    public List<ChatMessage> messages(String roomId, long chunk) {
        var room = room(roomId);
        var query = new Query();
        query.addCriteria(Criteria.where("room.$id").is(new ObjectId(room.getId())).and(CHAT_CHUNK_NUMBER_KEY).is(chunk));
        var chatChunk = mongoTemplate.findOne(query, ChatChunk.class);

        if (chatChunk == null) return List.of();

        return chatChunk.getMessages();
    }

    private Function<Throwable, Void> logExpecionally(String message) {
        return ex -> {
            log.error(message, (Throwable) null);
            return null;
        };
    }
}

