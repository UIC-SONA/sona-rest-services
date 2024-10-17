package ec.gob.conagopare.sona.repositories;

import ec.gob.conagopare.sona.models.RecoveryPasswordCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecoveryPasswordCodeRepository extends JpaRepository<RecoveryPasswordCode, UUID> {
    Optional<RecoveryPasswordCode> findByCode(@Param("code") String code);

    List<RecoveryPasswordCode> findAllByCreatedAtBefore(@Param("nowMinusMinutes") LocalDateTime nowMinusMinutes);

    default void deleteExpiredTokens(long minutes) {
        var nowMinusMinutes = LocalDateTime.now().minusMinutes(minutes);
        var expiredTokens = findAllByCreatedAtBefore(nowMinusMinutes);
        deleteAll(expiredTokens);
    }

}

