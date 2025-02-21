package ec.gob.conagopare.sona.modules.user.service;

import static org.mockito.Mockito.*;

import com.google.firebase.messaging.*;
import ec.gob.conagopare.sona.application.firebase.messaging.DeviceToken;
import ec.gob.conagopare.sona.application.firebase.messaging.DeviceTokenRepository;
import ec.gob.conagopare.sona.modules.user.models.User;
import ec.gob.conagopare.sona.modules.user.repositories.UserRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceUnitTest {

    @Mock
    private DeviceTokenRepository repository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FirebaseMessaging messaging;

    @InjectMocks
    private NotificationService notificationService;

    private Jwt jwt;

    @BeforeEach
    void setup() {
        jwt = mock(Jwt.class);
        lenient().when(jwt.getSubject()).thenReturn("user-keycloak-id");
    }

    @Test
    void testSuscribe_NewToken() {
        var token = "device-token";
        var user = new User();
        when(repository.findByToken(token)).thenReturn(Optional.empty());
        when(userRepository.findByKeycloakId(jwt.getSubject())).thenReturn(Optional.of(user));

        notificationService.suscribe(token, jwt);

        verify(repository).save(any(DeviceToken.class));
    }

    @Test
    void testSuscribe_ExistingToken() {
        var token = "device-token";
        var deviceToken = new DeviceToken();
        when(repository.findByToken(token)).thenReturn(Optional.of(deviceToken));

        notificationService.suscribe(token, jwt);

        verify(repository).save(deviceToken);
    }

    @Test
    void testUnsuscribe() {
        var token = "device-token";
        var deviceToken = new DeviceToken();
        when(repository.findByTokenAndUserKeycloakId(token, jwt.getSubject())).thenReturn(Optional.of(deviceToken));

        notificationService.unsuscribe(token, jwt);

        verify(repository).delete(deviceToken);
    }

    @Test
    void testSend_UserWithoutTokens() throws FirebaseMessagingException {
        when(repository.findUserTokens(1L)).thenReturn(Collections.emptyList());

        notificationService.send(1L, "Title", "Body");

        verify(messaging, never()).sendEachForMulticast(any(MulticastMessage.class));
    }

    @Test
    void testSendToMultipleUsers() throws FirebaseMessagingException {
        var userIds = List.of(1L, 2L);
        var title = "Test Title";
        var body = "Test Body";
        var tokens = List.of("token1", "token2");
        var response = buildBatchResponse();

        when(repository.findUsersTokens(userIds)).thenReturn(tokens);
        when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(response);

        notificationService.send(userIds, title, body);

        verify(repository).findUsersTokens(userIds);
    }

    @Test
    void testSendAll() throws FirebaseMessagingException {
        var title = "Global Notification";
        var body = "This is a test message for all users.";
        var tokens = List.of("token1", "token2", "token3");
        var response = buildBatchResponse();

        when(repository.findAllTokens()).thenReturn(tokens);
        when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(response);

        notificationService.sendAll(title, body);

        verify(repository).findAllTokens();
    }

    @Test
    void testSend_WithTokens() throws FirebaseMessagingException {
        var tokens = List.of("token1", "token2");
        when(repository.findUserTokens(1L)).thenReturn(tokens);

        var response = buildBatchResponse();
        when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(response);

        notificationService.send(1L, "Title", "Body");

        verify(messaging).sendEachForMulticast(any(MulticastMessage.class));
    }

    private static @NotNull BatchResponse buildBatchResponse() {
        return new BatchResponse() {
            @Override
            public List<SendResponse> getResponses() {
                return List.of();
            }

            @Override
            public int getSuccessCount() {
                return 0;
            }

            @Override
            public int getFailureCount() {
                return 0;
            }
        };
    }

    @Test
    void testCleanupUnusedTokens() {
        when(repository.deleteByRefreshedAtBefore(any(LocalDateTime.class))).thenReturn(5);

        notificationService.cleanupUnusedTokens();

        verify(repository).deleteByRefreshedAtBefore(any(LocalDateTime.class));
    }

}
