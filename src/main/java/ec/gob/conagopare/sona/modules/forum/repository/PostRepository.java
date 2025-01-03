package ec.gob.conagopare.sona.modules.forum.repository;

import ec.gob.conagopare.sona.modules.forum.models.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PostRepository extends MongoRepository<Post, String> {

    Page<Post> findAllByContentContainingIgnoreCase(String search, Pageable pageable);

    Page<Post> findAllByAuthor(Long author, Pageable pageable);

    Page<Post> findAllByAuthorAndContentContainingIgnoreCase(Long author, String search, Pageable pageable);

}
