package ec.gob.conagopare.sona.modules.content.repositories;

import ec.gob.conagopare.sona.modules.content.models.DidacticContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DidacticContentRepository extends JpaRepository<DidacticContent, UUID> {
}
