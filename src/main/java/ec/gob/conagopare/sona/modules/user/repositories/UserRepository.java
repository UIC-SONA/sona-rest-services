package ec.gob.conagopare.sona.modules.user.repositories;

import ec.gob.conagopare.sona.modules.user.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByKeycloakId(String keykloaId);

    boolean existsByKeycloakId(String keykloaId);
}