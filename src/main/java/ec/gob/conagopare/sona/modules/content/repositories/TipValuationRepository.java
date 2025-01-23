package ec.gob.conagopare.sona.modules.content.repositories;

import ec.gob.conagopare.sona.modules.content.models.TipValuation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TipValuationRepository extends JpaRepository<TipValuation, Long> {

    @Query("SELECT tv.valuation FROM TipValuation tv WHERE tv.tip.id = ?1 AND tv.user.id = ?2")
    Integer fingUserValuation(UUID tipId, Long userId);

    Optional<TipValuation> findByTipIdAndUserId(UUID tipId, Long userId);

    @Query("SELECT tv.valuation FROM TipValuation tv WHERE tv.tip.id = ?1")
    List<Integer> findValuations(UUID tipId);


}
