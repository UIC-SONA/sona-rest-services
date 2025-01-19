package ec.gob.conagopare.sona.application.firebase.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    int deleteByRefreshedAtBefore(LocalDateTime usedAtBefore);

    @Query("select dt.token from DeviceToken dt")
    List<String> findAllTokens();

    @Query("select dt.token from DeviceToken dt where dt.id = :userId")
    List<String> findUserTokens(Long userId);

    @Query("select dt.token from DeviceToken dt where dt.id in :usersIds")
    List<String> findUsersTokens(List<Long> userIds);

    Optional<DeviceToken> findByToken(String token);

    Optional<DeviceToken> findByTokenAndUserKeycloakId(String token, String subject);


}
