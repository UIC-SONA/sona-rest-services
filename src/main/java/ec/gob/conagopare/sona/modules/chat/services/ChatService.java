package ec.gob.conagopare.sona.modules.chat.services;

import ec.gob.conagopare.sona.modules.chat.dto.MessageSent;
import ec.gob.conagopare.sona.modules.chat.models.ChatMessage;
import ec.gob.conagopare.sona.modules.chat.models.ChatRoom;
import ec.gob.conagopare.sona.modules.chat.models.ChatRoomType;
import ec.gob.conagopare.sona.modules.chat.repositories.ChatMessageRepository;
import ec.gob.conagopare.sona.modules.chat.repositories.ChatRoomRepository;
import ec.gob.conagopare.sona.modules.user.models.User;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.concurrent.CompletableFuture.runAsync;

@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class ChatService {

    private final SimpMessagingTemplate messaging;
    private final UserService userService;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;


    public ChatMessage send(@Valid MessageSent message, Jwt jwt) {
        var user = userService.getUser(jwt);
        var chatRoom = room(message.getChatRoomId());

        if (!chatRoom.getParticipants().contains(user.getId())) {
            throw ApiError.forbidden("No tienes permiso para enviar mensajes a esta sala de chat");
        }

        var chatMessage = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(user.getId())
                .content(message.getContent())
                .timestamp(LocalDateTime.now())
                .build());

        runAsync(() -> messaging.convertAndSend("/chat.room." + chatRoom.getId(), chatMessage));
        for (var participant : chatRoom.getParticipants()) {
            runAsync(() -> messaging.convertAndSend("/chat.inbox." + participant, chatMessage));
        }

        return chatMessage;
    }

    public ChatRoom room(String chatRoomId) {
        return chatRoomRepository.findById(chatRoomId).orElseThrow(() -> ApiError.notFound("No se encontró la sala de chat con el id"));
    }

    public List<ChatRoom> rooms(Jwt jwt) {
        var user = userService.getUser(jwt);
        return chatRoomRepository.findAllByParticipantsContaining(List.of(user.getId()));
    }

    public ChatRoom room(Long userId, Jwt jwt) {
        var user = userService.getUser(jwt);
        var recipient = userService.getUser(userId);
        return findOrCreatePrivateRoom(user.getId(), recipient.getId());
    }

    private ChatRoom findOrCreatePrivateRoom(Long senderId, Long recipientId) {
        return chatRoomRepository
                .findByParticipantsAndType(List.of(senderId, recipientId), ChatRoomType.PRIVATE)
                .orElseGet(() -> createPrivateRoom(senderId, recipientId));
    }

    private ChatRoom createPrivateRoom(Long senderId, Long recipientId) {
        var newRoom = ChatRoom.builder()
                .type(ChatRoomType.PRIVATE)
                .participants(List.of(senderId, recipientId))
                .name("Private Chat between " + senderId + " and " + recipientId)
                .build();

        return chatRoomRepository.save(newRoom);
    }

    public List<User> users(Jwt jwt) {
        return userService.users();
    }
}
