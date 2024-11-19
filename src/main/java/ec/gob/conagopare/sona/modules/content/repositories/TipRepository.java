package ec.gob.conagopare.sona.modules.content.repositories;


import ec.gob.conagopare.sona.modules.content.models.Tip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TipRepository extends JpaRepository<Tip, UUID> {
    List<Tip> findAllByActiveTrue();

    @Query("SELECT image FROM Tip WHERE id = :id")
    Optional<String> getImagePathById(UUID id);
}
