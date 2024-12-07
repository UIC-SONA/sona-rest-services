package ec.gob.conagopare.sona.modules.chat.services;

import ec.gob.conagopare.sona.application.common.utils.MongoUtils;
import ec.gob.conagopare.sona.modules.chat.dto.ChatMessageSent;
import ec.gob.conagopare.sona.modules.chat.models.*;
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
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;

import static java.util.concurrent.CompletableFuture.runAsync;

@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class ChatService {

    private static final String CHAT_CHUNK_NUMBER_KEY = "number";
    private static final String CHAT_CHUNK_ROOM_KEY = "room.$id";

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
                .createdAt(Instant.now())
                .type(ChatMessageType.TEXT)
                .build();

        addMessage(chatRoom, chatMessage);

        var chatMessageSent = ChatMessageSent.builder()
                .requestId(requestId)
                .roomId(roomId)
                .message(chatMessage)
                .build();

        var roomDestination = "/topic/chat.room." + roomId;
        runAsync(() -> messaging.convertAndSend(roomDestination, chatMessageSent)).exceptionally(logExpecionally("Error enviando mensaje a la sala de chat"));

        for (var participant : chatRoom.getParticipants()) {
            runAsync(() -> messaging.convertAndSend("/topic/chat.inbox." + participant, chatMessageSent)).exceptionally(logExpecionally("Error enviando mensaje a la bandeja de entrada"));
        }

        return chatMessageSent;
    }

    public ChatRoom room(String chatRoomId) {
        return roomRepository.findById(chatRoomId).orElseThrow(() -> ApiError.notFound("No se encontró la sala de chat con el id"));
    }

    public List<ChatRoom> rooms(Jwt jwt) {
        var user = userService.getUser(jwt);
        assert user.getId() != null;
        return roomRepository.findByParticipant(user.getId())
                .stream()
                .filter(room -> existsChunk(room.getId()))
                .toList();
    }


    public List<ChatMessage> messages(String roomId, long chunk) {
        var query = new Query();
        query.addCriteria(roomCriteria(roomId).and(CHAT_CHUNK_NUMBER_KEY).is(chunk));
        var chatChunk = mongoTemplate.findOne(query, ChatChunk.class);

        return chatChunk == null ? List.of() : chatChunk.getMessages();
    }

    public ChatMessage lastMessage(String roomId) {
        String[] pipeline = {
                "{ $match: { \"" + CHAT_CHUNK_ROOM_KEY + "\": { $oid: '" + roomId + "' } } }",
                "{ $sort: { " + CHAT_CHUNK_NUMBER_KEY + ": -1 } }",
                "{ $limit: 1 }",
                "{ $project: { lastMessage: { $last: '$messages' } } }"
        };

        var result = mongoTemplate.getCollection(mongoTemplate.getCollectionName(ChatChunk.class))
                .aggregate(MongoUtils.toDocuments(pipeline))
                .first();

        if (result == null) return null;

        var lastMessage = result.get("lastMessage", Document.class);
        return mongoTemplate.getConverter().read(ChatMessage.class, lastMessage);
    }

    public ChatRoom room(Long userId, Jwt jwt) {
        var user = userService.getUser(jwt);
        var recipient = userService.getUser(userId);
        return findOrCreatePrivateRoom(user.getId(), recipient.getId());
    }

    public long chunkCount(String roomId) {
        var query = new Query();
        query.addCriteria(roomCriteria(roomId));
        return mongoTemplate.count(query, ChatChunk.class);
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

    private boolean existsChunk(String roomId) {
        var query = new Query();
        query.addCriteria(roomCriteria(roomId));
        return mongoTemplate.exists(query, ChatChunk.class);
    }


    public long getDocumentSize(String id, Class<?> collectionType) {
        String[] pipeline = {
                "{ $match: { _id: { $oid: '" + id + "' } } }",
                "{ $project: { _id: 0, size: { $bsonSize: '$$ROOT' } } }"
        };

        var result = mongoTemplate.getCollection(mongoTemplate.getCollectionName(collectionType))
                .aggregate(MongoUtils.toDocuments(pipeline))
                .first();

        if (result == null) return 0;

        var sizeValue = result.get("size", Number.class);
        return sizeValue != null ? sizeValue.longValue() : 0;
    }

    private void addMessage(ChatRoom chatRoom, ChatMessage message) {

        var query = new Query();
        query.addCriteria(roomCriteria(chatRoom.getId()));
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

        var result = mongoTemplate.updateFirst(query, update, ChatChunk.class);

        if (result.getModifiedCount() == 0) {
            throw ApiError.internalServerError("No se pudo agregar el mensaje al chat, resultado de la operación: " + result);
        }
    }


    private static Criteria roomCriteria(String roomId) {
        return Criteria.where(CHAT_CHUNK_ROOM_KEY).is(new ObjectId(roomId));
    }

    private static Function<Throwable, Void> logExpecionally(String message) {
        return ex -> {
            log.error(message, (Throwable) null);
            return null;
        };
    }
}

