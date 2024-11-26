package ec.gob.conagopare.sona.modules.chat.controllers;

import ec.gob.conagopare.sona.modules.chat.dto.MessageSent;
import ec.gob.conagopare.sona.modules.chat.services.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class RealTimeChatController {

    private final ChatService service;
    private final SimpMessagingTemplate messaging;

    @PostMapping("/send")
    public void send(@RequestBody MessageSent message, @AuthenticationPrincipal Jwt jwt) {
        var chatMessage = service.send(message, jwt);
        var chatRoom = chatMessage.getChatRoom();
        messaging.convertAndSend("/chat.room." + chatRoom.getId(), chatMessage);
        for (var participant : chatRoom.getParticipants()) {
            messaging.convertAndSend("/chat.inbox." + participant, chatMessage);
        }
    }

    @MessageExceptionHandler
    public void handleException(Exception e) {
        log.error("Error processing message", e);
        messaging.convertAndSend("/chat.error", e.getMessage());
    }

}
