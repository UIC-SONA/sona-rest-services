package ec.gob.conagopare.sona.modules.forum.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "forum")
public class Forum extends ByAuthor<Long> implements Persistable<String> {

    public static final String COMMENT_FIELD = "comments";
    public static final String LIKED_BY_FIELD = "likedBy";

    @Id
    private String id;
    private String content;
    private List<Comment> comments = new ArrayList<>();
    private List<Long> likedBy = new ArrayList<>();
    private List<Long> reportedBy = new ArrayList<>();

    private Instant createdAt;

    public static Comment newComment(String content, Long author, boolean isAnonymous) {
        var comment = new Comment();
        comment.setId(UUID.randomUUID().toString());
        comment.setContent(content);
        comment.setAuthor(author);
        comment.setAnonymous(isAnonymous);
        comment.setCreatedAt(Instant.now());
        return comment;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Comment extends ByAuthor<Long> {
        public static final String LIKED_BY_FIELD = "likedBy";
        public static final String REPORTED_BY_FIELD = "reportedBy";

        private String id;
        private String content;
        private Instant createdAt;
        private List<Long> likedBy = new ArrayList<>();
        private List<Long> reportedBy = new ArrayList<>();
    }
}