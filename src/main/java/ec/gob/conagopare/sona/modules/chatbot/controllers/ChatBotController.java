package ec.gob.conagopare.sona.modules.chatbot.controllers;

import ec.gob.conagopare.sona.modules.chatbot.models.PromptResponses;
import ec.gob.conagopare.sona.modules.chatbot.service.ChatBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatBotController {

    private final ChatBotService service;

    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/send-message")
    public ResponseEntity<PromptResponses> sendMessage(@RequestParam String prompt, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.sendMessage(jwt.getSubject(), prompt));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/history")
    public ResponseEntity<List<PromptResponses>> getChatHistory(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.getChatHistory(jwt.getSubject()));
    }

}