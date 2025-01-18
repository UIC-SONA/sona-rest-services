package ec.gob.conagopare.sona.application.firebase.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    int deleteByRefreshedAtBefore(LocalDateTime usedAtBefore);

    List<DeviceToken> findByUserId(Long userId);

    Optional<DeviceToken> findByToken(String token);

    Optional<DeviceToken> findByTokenAndUserKeycloakId(String token, String subject);
}
