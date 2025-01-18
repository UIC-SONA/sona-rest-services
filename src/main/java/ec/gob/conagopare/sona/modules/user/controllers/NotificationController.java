package ec.gob.conagopare.sona.modules.user.controllers;


import ec.gob.conagopare.sona.modules.user.service.NotificationService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Getter
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @PostMapping("/suscribe")
    public ResponseEntity<Void> suscribe(
            @RequestParam String token,
            @AuthenticationPrincipal Jwt jwt
    ) {
        service.suscribe(token, jwt);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unsuscribe")
    public ResponseEntity<Void> unsuscribe(
            @RequestParam String token,
            @AuthenticationPrincipal Jwt jwt
    ) {
        service.unsuscribe(token, jwt);
        return ResponseEntity.ok().build();
    }
}
