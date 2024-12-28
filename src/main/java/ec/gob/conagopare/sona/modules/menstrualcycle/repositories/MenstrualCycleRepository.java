package ec.gob.conagopare.sona.modules.menstrualcycle.repositories;

import ec.gob.conagopare.sona.modules.menstrualcycle.models.CycleData;
import ec.gob.conagopare.sona.modules.user.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MenstrualCycleRepository extends JpaRepository<CycleData, UUID> {
    Optional<CycleData> findByUser(User user);
}
