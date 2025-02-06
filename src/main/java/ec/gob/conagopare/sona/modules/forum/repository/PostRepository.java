package ec.gob.conagopare.sona.modules.forum.repository;

import ec.gob.conagopare.sona.modules.forum.models.Post;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface PostRepository extends MongoRepository<Post, String> {

}
