package ec.gob.conagopare.sona.repositories;

import ec.gob.conagopare.sona.models.Authority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorityRepository extends JpaRepository<Authority, Long> {

    Optional<Authority> findByName(String name);

    List<Authority> findAllByNameIn(List<String> names);

}
