package ec.gob.conagopare.sona.modules.menstrualcalendar.repositories;

import ec.gob.conagopare.sona.modules.menstrualcalendar.models.MenstrualCycle;
import ec.gob.conagopare.sona.modules.user.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MenstrualCycleRepository extends JpaRepository<MenstrualCycle, UUID> {
    Optional<MenstrualCycle> findByUser(User user);
}
