package ec.gob.conagopare.sona.modules.user.controllers;


import ec.gob.conagopare.sona.modules.user.service.NotificationService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Getter
@RestController
@RequestMapping("/notifcation")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @PostMapping("/susbribe")
    public ResponseEntity<Void> suscribe(
            @RequestParam String token,
            @AuthenticationPrincipal Jwt jwt
    ) {
        service.suscribe(token, jwt);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unsusbribe")
    public ResponseEntity<Void> unsuscribe(
            @RequestParam String token,
            @AuthenticationPrincipal Jwt jwt
    ) {
        service.unsuscribe(token, jwt);
        return ResponseEntity.ok().build();
    }
}
