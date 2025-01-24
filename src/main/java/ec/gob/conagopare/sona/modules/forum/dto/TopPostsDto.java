package ec.gob.conagopare.sona.modules.forum.dto;

import ec.gob.conagopare.sona.modules.forum.models.Post;
import lombok.Data;

@Data
public class TopPostsDto {
    private Post mostLikedPost;
    private Post mostCommentedPost;
}

