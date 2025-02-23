package ec.gob.conagopare.sona.modules.chat.services;

import ec.gob.conagopare.sona.application.common.utils.FileUtils;
import ec.gob.conagopare.sona.modules.chat.dto.ChatMessagePayload;
import ec.gob.conagopare.sona.modules.chat.dto.ReadMessages;
import ec.gob.conagopare.sona.modules.chat.models.*;
import ec.gob.conagopare.sona.modules.chat.repositories.ChatRoomRepository;
import ec.gob.conagopare.sona.modules.content.services.NotificationService;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.jakarta.validations.ContentType;
import io.github.luidmidev.jakarta.validations.FileSize;
import io.github.luidmidev.jakarta.validations.Image;
import io.github.luidmidev.springframework.web.problemdetails.ProblemDetails;
import io.github.luidmidev.storage.Storage;
import io.github.luidmidev.storage.Stored;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private static final String USERS_CHATS_PATH = "users/%d/chats/%s/%s";
    private static final String MESSAGES = "messages";

    private final SimpMessagingTemplate messaging;
    private final MongoTemplate mongoTemplate;
    private final UserService userService;
    private final ChatRoomRepository roomRepository;
    private final NotificationService notificationService;
    private final Storage storage;

    @PreAuthorize("isAuthenticated()")
    public ChatMessagePayload sendMessage(@NotEmpty String message, String roomId, String requestId, Jwt jwt) {
        var user = userService.getUser(jwt);
        var room = room(roomId);
        var chatMessage = ChatMessage.now(message, user.getId(), ChatMessageType.TEXT);
        return sendMessageToSuscribers(requestId, room, chatMessage);
    }

    @PreAuthorize("isAuthenticated()")
    public ChatMessagePayload sendImage(
            @Image
            @FileSize(value = 25, unit = FileSize.Unit.MB)
            MultipartFile file,

            String roomId,
            String requestId,
            Jwt jwt
    ) throws IOException {
        return sendFile(file, roomId, requestId, jwt, ChatMessageType.IMAGE, "images");
    }

    @PreAuthorize("isAuthenticated()")
    public ChatMessagePayload sendVoice(
            @ContentType("audio/*")
            @FileSize(value = 25, unit = FileSize.Unit.MB)
            MultipartFile file,

            String roomId,

            String requestId,

            Jwt jwt

    ) throws IOException {
        return sendFile(file, roomId, requestId, jwt, ChatMessageType.VOICE, "voices");
    }

    @PreAuthorize("isAuthenticated()")
    private ChatMessagePayload sendFile(
            MultipartFile file,
            String roomId,
            String requestId,
            Jwt jwt,
            ChatMessageType type,
            String dir
    ) throws IOException {

        var user = userService.getUser(jwt);
        var room = room(roomId);

        var filePath = storage.store(
                file.getInputStream(),
                FileUtils.factoryUUIDFileName(file.getOriginalFilename()),
                String.format(USERS_CHATS_PATH, user.getId(), room.getId(), dir)
        );

        var chatMessage = ChatMessage.now(filePath, user.getId(), type);
        return sendMessageToSuscribers(requestId, room, chatMessage);
    }

    public Stored resource(String id) throws IOException {
        return storage.download(id).orElseThrow(ProblemDetails::notFound);
    }

    private ChatMessagePayload sendMessageToSuscribers(String requestId, ChatRoom room, ChatMessage chatMessage) {
        var roomId = room.getId();
        addMessage(room, chatMessage);

        var chatMessageSent = ChatMessagePayload.builder()
                .requestId(requestId)
                .roomId(roomId)
                .message(chatMessage)
                .build();

        for (var participant : room.getParticipants()) {
            runAsync(() -> {
                messaging.convertAndSend("/topic/chat.inbox." + participant, chatMessageSent);
                if (participant.equals(chatMessage.getSentBy())) return;
                notificationService.send(participant,
                        "Tienes un nuevo mensaje en tu calendario menstrual",
                        "Ingresa a la aplicación para revisarla."
                );
            }).exceptionally(logExpecionally("Error enviando mensaje a la bandeja de entrada"));
        }

        return chatMessageSent;
    }

    @PreAuthorize("isAuthenticated()")
    public void read(String roomId, List<String> messagesIds, Jwt jwt) {
        log.info("Marking messages as read: roomId={}, messagesIds={}", roomId, messagesIds);

        var user = userService.getUser(jwt);
        var room = room(roomId);
        var userId = user.getId();

        if (!room.getParticipants().contains(userId)) {
            throw ProblemDetails.forbidden("No tienes permiso para leer mensajes en esta sala de chat");
        }

        var readBy = ChatMessage.ReadBy.now(userId);
        var query = new Query()
                .addCriteria(chunksOf(roomId).and("messages.id").in(messagesIds));

        var update = new Update()
                .addToSet("messages.$[message].readBy", readBy)
                .filterArray(Criteria
                        .where("message._id").in(messagesIds)
                        .and("message.sentBy").ne(userId)
                        .and("message.readBy.participantId").ne(userId)
                );

        var result = mongoTemplate.updateMulti(query, update, ChatChunk.class);

        if (result.getModifiedCount() == 0) {
            log.warn("No se marcaron mensajes como leídos");
            return;
        }

        var readMessages = ReadMessages.builder()
                .roomId(roomId)
                .readBy(readBy)
                .messageIds(messagesIds)
                .build();

        for (var participant : room.getParticipants()) {
            runAsync(() -> messaging.convertAndSend("/topic/chat.inbox." + participant + ".read", readMessages)).exceptionally(logExpecionally("Error enviando mensajes leídos a la bandeja de entrada"));
        }
    }

    @PreAuthorize("isAuthenticated()")
    public ChatRoom room(String chatRoomId) {
        return roomRepository.findById(chatRoomId).orElseThrow(() -> ProblemDetails.notFound("No se encontró la sala de chat"));
    }

    @PreAuthorize("isAuthenticated()")
    public List<ChatRoom> rooms(Jwt jwt) {
        var user = userService.getUser(jwt);
        assert user.getId() != null;

        return roomRepository.findByParticipant(user.getId())
                .stream()
                .filter(room -> existsChunk(room.getId()))
                .toList();
    }


    @PreAuthorize("isAuthenticated()")
    public List<ChatMessage> messages(String roomId, long chunk) {
        var query = new Query()
                .addCriteria(chunksOf(roomId).and(CHAT_CHUNK_NUMBER_KEY).is(chunk));

        var chatChunk = mongoTemplate.findOne(query, ChatChunk.class);
        return chatChunk == null ? List.of() : chatChunk.getMessages();
    }

    @PreAuthorize("isAuthenticated()")
    public ChatMessage lastMessage(String roomId) {
        var aggregate = Aggregation.newAggregation(
                Aggregation.match(chunksOf(roomId)),
                Aggregation.sort(Sort.Direction.DESC, CHAT_CHUNK_NUMBER_KEY),
                Aggregation.limit(1),
                Aggregation.project()
                        .and(ArrayOperators.Last.lastOf(MESSAGES)).as("lastMessage")
        );

        var result = mongoTemplate.aggregate(aggregate, ChatChunk.class, Document.class).getUniqueMappedResult();

        if (result == null) return null;

        var lastMessage = result.get("lastMessage", Document.class);
        return mongoTemplate.getConverter().read(ChatMessage.class, lastMessage);
    }


    /**
     *     private ChatMessage message(String roomId, String messageId) {
     *         log.info("Searching for message: roomId={}, messageId={}", roomId, messageId);
     *         var aggregate = Aggregation.newAggregation(
     *                 Aggregation.match(chunksOf(roomId)),
     *                 Aggregation.unwind(MESSAGES),
     *                 Aggregation.match(Criteria.where(MESSAGES + "._id").is(messageId)),
     *                 Aggregation.project()
     *                         .and(MESSAGES).as("message")
     *         );
     *         var result = mongoTemplate.aggregate(aggregate, ChatChunk.class, Document.class)
     *                 .getUniqueMappedResult();
     *         if (result == null) {
     *             log.warn("Message not found: roomId={}, messageId={}", roomId, messageId);
     *             throw ProblemDetails.notFound("No se encontró el mensaje");
     *         }
     *         var messageDoc = result.get("message", Document.class);
     *         return mongoTemplate.getConverter().read(ChatMessage.class, messageDoc);
     *     }
     * @param userId id del usuario con el que se quiere iniciar un chat
     * @param jwt token de autenticación
     * @return ChatRoom
     */
    @PreAuthorize("isAuthenticated()")
    public ChatRoom room(Long userId, Jwt jwt) {
        var user = userService.getUser(jwt);
        var recipient = userService.getUser(userId);
        return findOrCreatePrivateRoom(user.getId(), recipient.getId());
    }

    @PreAuthorize("isAuthenticated()")
    public long chunkCount(String roomId) {
        var query = new Query()
                .addCriteria(chunksOf(roomId));

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
        var query = new Query()
                .addCriteria(chunksOf(roomId));

        return mongoTemplate.exists(query, ChatChunk.class);
    }


    private long getChunkSize(String id) {
        var aggregate = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("_id").is(new ObjectId(id))),
                Aggregation.project()
                        .andExclude("_id")
                        .and(context -> new Document("$bsonSize", Aggregation.ROOT)).as("size")
        );

        var result = mongoTemplate.aggregate(aggregate, ChatChunk.class, Document.class).getUniqueMappedResult();

        if (result == null) return 0;

        var sizeValue = result.get("size", Number.class);
        return sizeValue != null ? sizeValue.longValue() : 0;
    }

    private void addMessage(ChatRoom chatRoom, ChatMessage message) {

        var query = new Query()
                .addCriteria(chunksOf(chatRoom.getId()))
                .with(Sort.by(Sort.Order.desc(CHAT_CHUNK_NUMBER_KEY)));

        var projectedQuery = Query.of(query);
        projectedQuery.fields()
                .include("_id")
                .include(CHAT_CHUNK_NUMBER_KEY);

        var latestChunk = mongoTemplate.findOne(projectedQuery, ChatChunk.class);

        if (latestChunk == null) {
            mongoTemplate.save(ChatChunk.withFirstMessage(chatRoom, 1, message));
            return;
        }

        var aproxSize = getChunkSize(latestChunk.getId());

        if (aproxSize > ChatChunk.MAX_CHUNK_SIZE) {
            mongoTemplate.save(ChatChunk.withFirstMessage(chatRoom, latestChunk.getNumber() + 1, message));
            return;
        }

        var update = new Update();
        update.push(MESSAGES, message);

        var result = mongoTemplate.updateFirst(query, update, ChatChunk.class);

        if (result.getModifiedCount() == 0) {
            throw ProblemDetails.internalServerError("No se pudo agregar el mensaje al chat, resultado de la operación: " + result);
        }
    }


    private static Criteria chunksOf(String roomId) {
        return Criteria.where(CHAT_CHUNK_ROOM_KEY).is(new ObjectId(roomId));
    }

    private static Function<Throwable, Void> logExpecionally(String message) {
        return ex -> {
            log.error(message, ex);
            return null;
        };
    }


}

