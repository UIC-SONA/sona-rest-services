package ec.gob.conagopare.sona.modules.chatbot.controllers;

import ec.gob.conagopare.sona.modules.chatbot.service.ChatBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatBotController {

    private final ChatBotService service;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/send-message")
    public SseEmitter sendMessage(@RequestParam String message, @AuthenticationPrincipal Jwt jwt) {
        return service.sendMessage(message, jwt);
    }

}
