package ec.gob.conagopare.sona.modules.chat.services;

import com.mongodb.client.result.UpdateResult;
import ec.gob.conagopare.sona.modules.chat.dto.ChatMessagePayload;
import ec.gob.conagopare.sona.modules.chat.dto.ReadMessages;
import ec.gob.conagopare.sona.modules.chat.models.*;
import ec.gob.conagopare.sona.modules.chat.repositories.ChatRoomRepository;
import ec.gob.conagopare.sona.modules.user.models.User;
import ec.gob.conagopare.sona.modules.user.service.NotificationService;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.storage.Storage;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestChatService {

    @Mock
    private SimpMessagingTemplate messaging;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private UserService userService;
    @Mock
    private ChatRoomRepository roomRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private Storage storage;

    @InjectMocks
    private ChatService chatService;

    @Captor
    private ArgumentCaptor<String> topicCaptor;
    @Captor
    private ArgumentCaptor<ChatMessagePayload> messagePayloadCaptor;

    private static final String ROOM_ID = ObjectId.get().toHexString();
    private static final String REQUEST_ID = "test-request-id";
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    private ChatRoom room;
    private User user;
    private Jwt jwt;
    private ChatMessage message;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .build();

        jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(String.valueOf(USER_ID))
                .build();

        room = ChatRoom.builder()
                .id(ROOM_ID)
                .name("Test Room")
                .type(ChatRoomType.PRIVATE)
                .participants(List.of(USER_ID, OTHER_USER_ID))
                .build();

        message = ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .message("Test message")
                .createdAt(Instant.now())
                .sentBy(USER_ID)
                .type(ChatMessageType.TEXT)
                .readBy(new ArrayList<>())
                .build();

    }


    @Test
    void sendMessage_ShouldSendMessageToAllParticipants() {

        var chunkMock = mock(ChatChunk.class);
        when(chunkMock.getId()).thenReturn(ObjectId.get().toHexString());

        when(userService.getUser(any(Jwt.class))).thenReturn(user);
        when(roomRepository.findById(anyString())).thenReturn(Optional.of(room));
        when(mongoTemplate.findOne(any(Query.class), eq(ChatChunk.class))).thenReturn(chunkMock);
        when(mongoTemplate.aggregate(any(Aggregation.class), eq(ChatChunk.class), eq(Document.class))).thenReturn(new AggregationResults<>(List.of(new Document("size", 5)), new Document()));
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(ChatChunk.class))).thenReturn(UpdateResult.acknowledged(1, 1L, null));

        chatService.sendMessage("Hello world", ROOM_ID, REQUEST_ID, jwt);

        verify(messaging, atLeast(1)).convertAndSend(topicCaptor.capture(), messagePayloadCaptor.capture());

        assertThat(messagePayloadCaptor.getValue().getMessage().getMessage()).isEqualTo("Hello world");
        assertThat(messagePayloadCaptor.getValue().getMessage().getType()).isEqualTo(ChatMessageType.TEXT);
        assertThat(messagePayloadCaptor.getValue().getMessage().getSentBy()).isEqualTo(USER_ID);
    }

    @Test
    void sendImage_ShouldStoreAndSendImage() throws IOException {
        var storedPath = "users/1/chats/test-room-id/images/test.jpg";
        var file = mock(MultipartFile.class);

        when(userService.getUser(any(Jwt.class))).thenReturn(user);
        when(roomRepository.findById(anyString())).thenReturn(Optional.of(room));
        when(file.getOriginalFilename()).thenReturn("test.jpg");
        when(file.getInputStream()).thenReturn(mock(InputStream.class));
        when(storage.store(any(InputStream.class), anyString(), anyString())).thenReturn(storedPath);
        when(mongoTemplate.save(any(ChatChunk.class))).thenReturn(mock(ChatChunk.class));

        chatService.sendImage(file, ROOM_ID, REQUEST_ID, jwt);

        verify(storage).store(any(InputStream.class), anyString(), contains("images"));
    }

    @Test
    void read_ShouldMarkMessagesAsRead() {
        var messageIds = List.of("msg-1", "msg-2");
        var updateResult = mock(UpdateResult.class);


        when(userService.getUser(any(Jwt.class))).thenReturn(user);
        when(roomRepository.findById(anyString())).thenReturn(Optional.of(room));
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq(ChatChunk.class))).thenReturn(updateResult);
        when(updateResult.getModifiedCount()).thenReturn(2L);

        chatService.read(ROOM_ID, messageIds, jwt);

        verify(messaging, atLeast(1)).convertAndSend(
                argThat(topic -> topic.startsWith("/topic/chat.inbox")),
                argThat((ReadMessages rm) ->
                        rm.getRoomId().equals(ROOM_ID) &&
                                rm.getMessageIds().equals(messageIds) &&
                                rm.getReadBy().getParticipantId().equals(USER_ID)
                )
        );
    }

    @Test
    void lastMessage_ShouldReturnLastMessage() {
        Document result = new Document("lastMessage", new Document());
        AggregationResults<Document> aggregationResults = mock(AggregationResults.class);

        when(mongoTemplate.aggregate(any(Aggregation.class), eq(ChatChunk.class), eq(Document.class)))
                .thenReturn(aggregationResults);
        when(aggregationResults.getUniqueMappedResult()).thenReturn(result);
        when(mongoTemplate.getConverter()).thenReturn(mock(MappingMongoConverter.class));
        when(mongoTemplate.getConverter().read(eq(ChatMessage.class), any(Document.class))).thenReturn(message);

        assertThat(chatService.lastMessage(ROOM_ID)).isEqualTo(message);
    }

    @Test
    void room_ShouldCreateNewRoomIfNotExists() {
        User recipient = User.builder().id(OTHER_USER_ID).build();

        when(userService.getUser(any(Jwt.class))).thenReturn(user);
        when(userService.getUser(anyLong())).thenReturn(recipient);
        when(roomRepository.findByParticipantsAndType(anyList(), eq(ChatRoomType.PRIVATE)))
                .thenReturn(Optional.empty());
        when(roomRepository.save(any(ChatRoom.class))).thenReturn(room);

        chatService.room(OTHER_USER_ID, jwt);

        verify(roomRepository).save(argThat((ChatRoom r) ->
                r.getParticipants().containsAll(List.of(USER_ID, OTHER_USER_ID)) &&
                        r.getType().equals(ChatRoomType.PRIVATE)
        ));
    }

    @Test
    void messages_ShouldReturnMessagesForChunk() {
        ChatChunk chatChunk = ChatChunk.builder()
                .id("chunk-1")
                .number(1L)
                .room(room)
                .messages(List.of(message))
                .build();

        when(mongoTemplate.findOne(any(Query.class), eq(ChatChunk.class)))
                .thenReturn(chatChunk);

        assertThat(chatService.messages(ROOM_ID, 1L))
                .hasSize(1)
                .containsExactly(message);
    }

    @Test
    void chunkCount_ShouldReturnNumberOfChunks() {
        when(mongoTemplate.count(any(Query.class), eq(ChatChunk.class))).thenReturn(5L);
        assertThat(chatService.chunkCount(ROOM_ID)).isEqualTo(5L);
    }

    @Test
    void rooms_ShouldReturnRooms() {
        when(userService.getUser(jwt)).thenReturn(user);
        when(roomRepository.findByParticipant(USER_ID)).thenReturn(List.of(room));
        when(mongoTemplate.exists(any(Query.class), eq(ChatChunk.class))).thenReturn(true);
        assertThat(chatService.rooms(jwt)).hasSize(1);
    }
}