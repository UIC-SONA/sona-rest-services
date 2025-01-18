package ec.gob.conagopare.sona.modules.user.repositories;

import ec.gob.conagopare.sona.modules.user.models.NotificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTokenRepository extends JpaRepository<NotificationToken, Long> {

    boolean existsByToken(String token);

    Optional<NotificationToken> findByTokenAndUserKeycloakId(String token, String keycloakId);


}
