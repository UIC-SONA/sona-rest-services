package ec.gob.conagopare.sona.modules.chat.controllers;

import ec.gob.conagopare.sona.modules.chat.dto.ChatMessageSent;
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
import java.util.UUID;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ChatController {

    private final ChatService service;

    @PostMapping("/send/{roomId}")
    public ResponseEntity<ChatMessageSent> send(
            @PathVariable String roomId,
            @RequestParam String requestId,
            @RequestBody String message,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(service.send(message, roomId, requestId, jwt));
    }

    @PutMapping("/room/{roomId}/read")
    public ResponseEntity<Void> read(
            @PathVariable String roomId,
            @RequestParam List<UUID> messages,
            @AuthenticationPrincipal Jwt jwt
    ) {
        service.read(roomId, messages, jwt);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoom>> rooms(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(service.rooms(jwt));
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<ChatRoom> room(
            @PathVariable String roomId
    ) {
        return ResponseEntity.ok(service.room(roomId));
    }

    @GetMapping("/user/{userId}/room")
    public ResponseEntity<ChatRoom> room(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(service.room(userId, jwt));
    }

    @GetMapping("/room/{roomId}/messages")
    public ResponseEntity<List<ChatMessage>> messages(
            @PathVariable String roomId,
            @RequestParam long chunk
    ) {
        return ResponseEntity.ok(service.messages(roomId, chunk));
    }

    @GetMapping("/room/{roomId}/last-message")
    public ResponseEntity<ChatMessage> lastMessage(
            @PathVariable String roomId
    ) {
        return ResponseEntity.ok(service.lastMessage(roomId));
    }

    @GetMapping("/room/{roomId}/chunk-count")
    public ResponseEntity<Long> chunkCount(
            @PathVariable String roomId
    ) {
        return ResponseEntity.ok(service.chunkCount(roomId));
    }

}
