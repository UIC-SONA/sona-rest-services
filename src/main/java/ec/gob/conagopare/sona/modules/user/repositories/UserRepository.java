package ec.gob.conagopare.sona.modules.user.repositories;

import ec.gob.conagopare.sona.modules.user.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByKeycloakId(String keykloaId);

    List<User> findAllByKeycloakIdIn(List<String> keycloakIds);

}
