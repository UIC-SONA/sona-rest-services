package ec.gob.conagopare.sona.application.firebase;

import com.google.firebase.messaging.*;
import ec.gob.conagopare.sona.application.firebase.messaging.DeviceToken;
import ec.gob.conagopare.sona.application.firebase.messaging.DeviceTokenRepository;
import ec.gob.conagopare.sona.modules.user.repositories.UserRepository;
import io.github.luidmidev.springframework.web.problemdetails.ProblemDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    public final DeviceTokenRepository repository;
    private final UserRepository userRepository;
    private final FirebaseMessaging messaging;

    @PreAuthorize("isAuthenticated()")
    public void suscribe(String token, Jwt jwt) {
        //
        var deviceToken = repository.findByToken(token)
                .orElseGet(() -> {
                    var user = userRepository.findByKeycloakId(jwt.getSubject()).orElseThrow(ProblemDetails::badRequest);
                    return DeviceToken.builder()
                            .token(token)
                            .user(user)
                            .build();
                });

        deviceToken.setRefreshedAt(LocalDateTime.now());
        repository.save(deviceToken);
    }

    @PreAuthorize("isAuthenticated()")
    public void unsuscribe(String token, Jwt jwt) {
        var saved = repository.findByTokenAndUserKeycloakId(token, jwt.getSubject());
        if (saved.isEmpty()) return;
        repository.delete(saved.get());
    }

    public void send(Long userId, String title, String body) {
        send(userId, title, body, Map.of());
    }


    public void send(Long userId, String title, String body, Map<String, String> data) {
        var tokens = repository.findUserTokens(userId);
        if (tokens.isEmpty()) {
            log.warn("Not found device tokes for user {}", userId);
            return;
        }
        internalSend(tokens, title, body, data);
    }

    public void send(List<Long> userIds, String title, String body) {
        send(userIds, title, body, Map.of());
    }

    public void send(List<Long> userIds, String title, String body, Map<String, String> data) {
        var tokens = repository.findUsersTokens(userIds);
        if (tokens.isEmpty()) return;
        internalSend(tokens, title, body, data);
    }

    public void sendAll(String title, String body) {
        sendAll(title, body, Map.of());
    }

    public void sendAll(String title, String body, Map<String, String> data) {
        var tokens = repository.findAllTokens();
        internalSend(tokens, title, body, data);
    }

    private void internalSend(Collection<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens.isEmpty()) {
            return;
        }
        var message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .addAllTokens(tokens)
                .build();
        try {
            var response = messaging.sendEachForMulticast(message);
            log.info("Successfully sent message: success count: {}, failure count: {}", "" + response.getSuccessCount(), "" + response.getFailureCount());
        } catch (FirebaseMessagingException e) {
            log.error("Error sending FCM message", e);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupUnusedTokens() {
        try {
            // Eliminar tokens que no se utilizan (no han sido renovados hace 3 meses)
            // Los tokens se renuevan cuando se abre la aplciación
            int deletedCount = repository.deleteByRefreshedAtBefore(LocalDateTime.now().minusMonths(3));
            log.info("Cleaned up {} unused tokens", deletedCount);
        } catch (Exception e) {
            log.error("Error during token cleanup", e);
        }
    }
}
