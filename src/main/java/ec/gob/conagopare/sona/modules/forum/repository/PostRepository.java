package ec.gob.conagopare.sona.modules.forum.repository;

import ec.gob.conagopare.sona.modules.forum.models.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PostRepository extends MongoRepository<Post, String> {

    List<Post> findAllByContentContainingIgnoreCase(String search, Sort sort);

    Page<Post> findAllByContentContainingIgnoreCase(String search, Pageable pageable);

    List<Post> findAllByAuthor(Long author, Sort sort);

    Page<Post> findAllByAuthor(Long author, Pageable pageable);

    List<Post> findAllByAuthorAndContentContainingIgnoreCase(Long author, String search, Sort sort);

    Page<Post> findAllByAuthorAndContentContainingIgnoreCase(Long author, String search, Pageable pageable);

}
