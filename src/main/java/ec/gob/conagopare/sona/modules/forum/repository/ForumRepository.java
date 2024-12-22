package ec.gob.conagopare.sona.modules.forum.repository;

import ec.gob.conagopare.sona.modules.forum.models.Forum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ForumRepository extends MongoRepository<Forum, String> {

    List<Forum> findAllByContentContainingIgnoreCase(String search, Sort sort);

    Page<Forum> findAllByContentContainingIgnoreCase(String search, Pageable pageable);

    List<Forum> findAllByAuthor(Long author, Sort sort);

    Page<Forum> findAllByAuthor(Long author, Pageable pageable);

    List<Forum> findAllByAuthorAndContentContainingIgnoreCase(Long author, String search, Sort sort);

    Page<Forum> findAllByAuthorAndContentContainingIgnoreCase(Long author, String search, Pageable pageable);

}
