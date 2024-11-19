package ec.gob.conagopare.sona.modules.content.repositories;


import ec.gob.conagopare.sona.modules.content.models.Tip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TipRepository extends JpaRepository<Tip, UUID> {
    List<Tip> findAllByActiveTrue();
}
