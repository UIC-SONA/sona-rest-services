package ec.gob.conagopare.sona.modules.content.repositories;


import ec.gob.conagopare.sona.modules.content.models.Tip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TipRepository extends JpaRepository<Tip, UUID> {

    Page<Tip> findAllByActiveTrue(Pageable pageable);

    @Query("SELECT image FROM Tip WHERE id = :id")
    Optional<String> getImagePathById(UUID id);

    @Query("SELECT  t FROM Tip t JOIN TipRate tr ON t.id = tr.tip.id GROUP BY t.id ORDER BY AVG(tr.value) DESC LIMIT :limit")
    List<Tip> topRating(int limit);
}
