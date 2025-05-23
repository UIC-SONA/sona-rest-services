package ec.gob.conagopare.sona.modules.tips.repositories;

import ec.gob.conagopare.sona.modules.tips.models.TipRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TipValuationRepository extends JpaRepository<TipRate, Long> {

    @Query("SELECT tv.value FROM TipRate tv WHERE tv.tip.id = ?1 AND tv.user.id = ?2")
    Integer fingUserValuation(UUID tipId, Long userId);

    Optional<TipRate> findByTipIdAndUserId(UUID tipId, Long userId);

    @Query("SELECT tv.value FROM TipRate tv WHERE tv.tip.id = ?1")
    List<Integer> findValuations(UUID tipId);

}
