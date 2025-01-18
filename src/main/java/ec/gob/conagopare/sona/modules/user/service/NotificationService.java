package ec.gob.conagopare.sona.modules.user.service;

import ec.gob.conagopare.sona.modules.user.models.NotificationToken;
import ec.gob.conagopare.sona.modules.user.repositories.NotificationTokenRepository;
import ec.gob.conagopare.sona.modules.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    public final NotificationTokenRepository repository;
    private final UserRepository userRepository;

    @PreAuthorize("isAuthenticated()")
    public void suscribe(String token, Jwt jwt) {
        if (repository.existsByToken(token)) {
            return;
        }
        var user = userRepository.findByKeycloakId(jwt.getSubject());
        if (user.isEmpty()) {
            return;
        }
        repository.save(NotificationToken.builder()
                .token(token)
                .lastUsedAt(LocalDateTime.now())
                .user(user.get())
                .build());
    }

    @PreAuthorize("isAuthenticated()")
    public void unsuscribe(String token, Jwt jwt) {
        if (repository.existsByToken(token)) {
            return;
        }

        var saved = repository.findByTokenAndUserKeycloakId(token, jwt.getSubject());
        if (saved.isEmpty()) {
            return;
        }

        repository.delete(saved.get());
    }
}
