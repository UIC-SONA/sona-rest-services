package ec.gob.conagopare.sona.modules.chat.controllers;

import ec.gob.conagopare.sona.modules.chat.dto.MessageSent;
import ec.gob.conagopare.sona.modules.chat.models.ChatChunk;
import ec.gob.conagopare.sona.modules.chat.models.ChatMessage;
import ec.gob.conagopare.sona.modules.chat.models.ChatRoom;
import ec.gob.conagopare.sona.modules.chat.services.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ChatController {

    private final ChatService service;

    @PostMapping("/send")
    public ResponseEntity<ChatMessage> send(@RequestBody MessageSent message, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.send(message, jwt));
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoom>> rooms(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.rooms(jwt));
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<ChatRoom> room(@PathVariable String roomId) {
        return ResponseEntity.ok(service.room(roomId));
    }

    @GetMapping("/user/{userId}/room")
    public ResponseEntity<ChatRoom> room(@PathVariable Long userId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.room(userId, jwt));
    }

    @GetMapping("/room/{roomId}/messages/{chunk}")
    public ResponseEntity<List<ChatMessage>> messages(@PathVariable String roomId, @PathVariable long chunk) {
        return ResponseEntity.ok(service.messages(roomId, chunk));
    }
}
