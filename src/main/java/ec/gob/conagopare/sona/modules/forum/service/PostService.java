package ec.gob.conagopare.sona.modules.forum.service;

import ec.gob.conagopare.sona.application.common.utils.MongoUtils;
import ec.gob.conagopare.sona.modules.forum.dto.NewComment;
import ec.gob.conagopare.sona.modules.forum.dto.PostDto;
import ec.gob.conagopare.sona.modules.forum.dto.TopPostsDto;
import ec.gob.conagopare.sona.modules.forum.models.ByAuthor;
import ec.gob.conagopare.sona.modules.forum.models.Post;
import ec.gob.conagopare.sona.modules.forum.models.Post.Comment;
import ec.gob.conagopare.sona.modules.forum.repository.PostRepository;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import ec.gob.conagopare.sona.modules.user.models.User;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.springframework.data.crud.core.services.StandardCrudService;
import io.github.luidmidev.springframework.data.crud.core.services.hooks.CrudHooks;
import io.github.luidmidev.springframework.data.crud.core.utils.StringUtils;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import jakarta.persistence.EntityManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.springframework.data.mongodb.core.query.Criteria.where;


@Slf4j
@Service
@Getter
@RequiredArgsConstructor
@SuppressWarnings("java:S1192")
public class PostService implements StandardCrudService<Post, PostDto, String, PostRepository> {

    private static final Set<Authority> PRIVILEGED_AUTHORITIES = Set.of(
            Authority.ADMIN,
            Authority.ADMINISTRATIVE
    );

    public static final String COMMENT_ARRAY_FILTER = "comment";
    public static final String COMMENT_ID_FILTER = "comment._id";

    private final PostRepository repository;
    private final EntityManager entityManager;
    private final MongoTemplate mongo;
    private final UserService userService;


    @Override
    public Class<Post> getEntityClass() {
        return Post.class;
    }

    @Override
    @SneakyThrows
    public void mapModel(PostDto dto, Post model) {
        if (!model.isNew()) {
            throw ApiError.badRequest("Las publicaciones no pueden ser modificadas");
        }

        var user = userService.getCurrentUser();
        var content = dto.getContent();
        var isAnonymous = solveAnonymous(user, dto.getAnonymous());

        model.setContent(content);
        model.setCreatedAt(Instant.now());
        model.setAuthor(user.getId());
        model.setAnonymous(isAnonymous);
    }

    @Override
    public void delete(String id) {
        var user = userService.getCurrentUser();
        var isPriviliged = isPriviliged(user);
        var query = isPriviliged ? isId(id) : isAuthor(id, user.getId());
        var result = mongo.remove(query, Post.class);
        if (result.getDeletedCount() == 0) {
            throw isPriviliged ? ApiError.notFound("Publicación no encontrada") : ApiError.forbidden("No tienes permisos para eliminar esta publicación");
        }
    }

    @PreAuthorize("isAuthenticated()")
    public Comment commentPost(String postId, NewComment newComment) {
        return updatePost(isId(postId), (update, user) -> {
            var content = newComment.getContent();
            var anonymous = solveAnonymous(user, newComment.getAnonymous());
            var comment = Post.newComment(content, user.getId(), anonymous);
            update.push(Post.COMMENTS_FIELD, comment);
            return comment;
        });
    }

    @PreAuthorize("isAuthenticated()")
    public void deleteComment(String postId, String commentId) {
        var user = userService.getCurrentUser();

        var criteria = where("_id").is(new ObjectId(postId));

        if (isPriviliged(user)) {
            criteria.and(Post.COMMENTS_FIELD).elemMatch(where("id").is(commentId));
        } else {
            criteria.andOperator(
                    new Criteria().orOperator(
                            where(Post.COMMENTS_FIELD).elemMatch(where("id").is(commentId).and(ByAuthor.AUTHOR_FIELD).is(user.getId())),
                            where(ByAuthor.AUTHOR_FIELD).is(user.getId())
                    )
            );
        }

        var query = Query.query(criteria);
        updatePost(query, update -> update.pull(Post.COMMENTS_FIELD, Query.query(where("id").is(commentId))));
    }

    @PreAuthorize("isAuthenticated()")
    public void likePost(String postId) {
        updatePost(isId(postId), (update, user) -> update.addToSet(Post.LIKED_BY_FIELD, user.getId()));
    }

    @PreAuthorize("isAuthenticated()")
    public void unlikePost(String postId) {
        updatePost(isId(postId), (update, user) -> update.pull(Post.LIKED_BY_FIELD, user.getId()));
    }

    @PreAuthorize("isAuthenticated()")
    public void reportPost(String postId) {
        updatePost(isId(postId), (update, user) -> update.addToSet(Post.REPORTED_BY_FIELD, user.getId()));
    }

    private <T> T updatePost(Query query, BiFunction<Update, User, T> updater) {
        var user = userService.getCurrentUser();
        return updatePost(query, update -> updater.apply(update, user));
    }

    private <T> T updatePost(Query query, Function<Update, T> updater) {
        var update = new Update();
        var returned = updater.apply(update);
        var result = mongo.updateFirst(query, update, Post.class);
        if (result.getModifiedCount() == 0) {
            log.warn("No se encontró la publicación para actualizar");
        }
        return returned;
    }

    @Override
    public Page<Post> internalPage(Pageable pageable) {
        return internalSearch(null, pageable);
    }

    @Override
    public Page<Post> internalSearch(String search, Pageable pageable) {
        return internalSearch(search, pageable, new LinkedMultiValueMap<>());
    }

    @Override
    public Page<Post> internalSearch(String search, Pageable pageable, MultiValueMap<String, String> filters) {
        var operations = new ArrayList<AggregationOperation>();

        operations.add(
                projectPost()
                        .andExpression("size(likedBy)").as("likesCount")
                        .andExpression("size(comments)").as("commentsCount")
        );

        var or = new ArrayList<Criteria>();

        if (!StringUtils.isBlank(search)) {
            or.add(where(Post.CONTENT_FIELD).regex(search, "i"));
        }

        var author = filters.getFirst("author");
        if (author != null) {
            or.add(where(ByAuthor.AUTHOR_FIELD).is(Long.parseLong(author)));
        }

        if (!or.isEmpty()) {
            operations.add(Aggregation.match(new Criteria().orOperator(or)));
        }
        return MongoUtils.getPage(mongo, pageable, operations, Post.class);
    }


    public void likeComment(String forumId, String commentId) {
        updatePost(isId(forumId), (update, user) -> update
                .addToSet(Post.COMMENTS_FIELD + ".$[" + COMMENT_ARRAY_FILTER + "]." + Post.Comment.LIKED_BY_FIELD, user.getId())
                .filterArray(where(COMMENT_ID_FILTER).is(commentId))
        );
    }

    public void unlikeComment(String forumId, String commentId) {
        updatePost(isId(forumId), (update, user) -> update
                .pull(Post.COMMENTS_FIELD + ".$[" + COMMENT_ARRAY_FILTER + "]." + Post.Comment.LIKED_BY_FIELD, user.getId())
                .filterArray(where(COMMENT_ID_FILTER).is(commentId))
        );
    }

    public void reportComment(String forumId, String commentId) {
        updatePost(isId(forumId), (update, user) -> update
                .addToSet(Post.COMMENTS_FIELD + ".$[" + COMMENT_ARRAY_FILTER + "]." + Post.Comment.REPORTED_BY_FIELD, user.getId())
                .filterArray(where(COMMENT_ID_FILTER).is(commentId))
        );
    }

    private static boolean solveAnonymous(User user, Boolean anonymous) {
        return anonymous == null ? user.isAnonymous() : anonymous;
    }

    private static boolean isPriviliged(User user) {
        return user
                .getAuthorities()
                .stream()
                .anyMatch(PRIVILEGED_AUTHORITIES::contains);
    }

    private static Query isAuthor(String id, Long author) {
        return Query.query(where("_id").is(new ObjectId(id)).and(ByAuthor.AUTHOR_FIELD).is(author));
    }

    private static Query isId(String id) {
        return Query.query(where("_id").is(new ObjectId(id)));
    }


    private final CrudHooks<Post, PostDto, String> hooks = new CrudHooks<>() {
        @Override
        public void onFind(Post entity) {
            var user = userService.getCurrentUser();
            setIfIsAuthor(entity, user);
        }

        @Override
        public void onFind(Iterable<Post> entities, Iterable<String> ids) {
            var user = userService.getCurrentUser();
            for (var entity : entities) {
                setIfIsAuthor(entity, user);
            }
        }

        @Override
        public void onPage(Page<Post> page) {
            var user = userService.getCurrentUser();
            for (var entity : page) {
                setIfIsAuthor(entity, user);
            }
        }

        private static void setIfIsAuthor(Post entity, User user) {
            entity.setIAmAuthor(entity.getRealAuthor().equals(user.getId()));
        }

    };

    @PreAuthorize("isAuthenticated()")
    public TopPostsDto topPosts() {
        var dto = new TopPostsDto();
        dto.setMostLikedPost(mostLiked());
        dto.setMostCommentedPost(mostCommented());
        return dto;
    }

    private Post mostLiked() {
        var aggregation = Aggregation.newAggregation(
                projectPost()
                        .andExpression("size(likedBy)").as("likesCount"),
                Aggregation.sort(Sort.Direction.DESC, "likesCount"),
                Aggregation.limit(1)
        );

        var results = mongo.aggregate(aggregation, "post", Post.class);

        return results.getUniqueMappedResult();
    }

    private Post mostCommented() {
        Aggregation aggregation = Aggregation.newAggregation(
                projectPost()
                        .andExpression("size(comments)").as("commentsCount"),
                Aggregation.sort(Sort.Direction.DESC, "commentsCount"),
                Aggregation.limit(1)
        );

        var results = mongo.aggregate(aggregation, mongo.getCollectionName(Post.class), Post.class);

        return results.getUniqueMappedResult();
    }

    @PreAuthorize("hasAnyRole('admin', 'administrative')")
    public Page<Comment> pageComments(String postId, String search, Pageable pageable, MultiValueMap<String, String> filters) {
        // Crear el pipeline de agregación
        var operations = new ArrayList<AggregationOperation>();

        // Filtrar por publicación si se especifica
        if (postId != null) {
            operations.add(Aggregation.match(where("_id").is(new ObjectId(postId))));
        }

        // Desenrollar los comentarios
        operations.add(Aggregation.unwind("comments"));

        // Proyectar los campos necesarios
        operations.add(Aggregation.project()
                .andExclude("_id")
                .and("comments._id").as("_id")
                .and("comments.author").as(ByAuthor.AUTHOR_FIELD)
                .and("comments.anonymous").as(ByAuthor.ANONYMOUS_FIELD)
                .and("comments.content").as(Comment.CONTENT_FIELD)
                .and("comments.createdAt").as(Comment.CREATED_AT_FIELD)
                .and("comments.likedBy").as(Comment.LIKED_BY_FIELD)
                .and("comments.reportedBy").as(Comment.REPORTED_BY_FIELD)
                .andExpression("size(comments.likedBy)").as("likesCount")
                .andExpression("size(comments.reportedBy)").as("reportsCount")
        );

        // Filtrar por autor si se especifica
        var authorId = filters.getFirst("authorId");
        if (authorId != null) {
            operations.add(Aggregation.match(
                    where(ByAuthor.AUTHOR_FIELD).is(Long.parseLong(authorId))
            ));
        }

        // Si hay búsqueda por texto, agregar filtro
        var normalizedSearch = StringUtils.normalize(search);
        if (normalizedSearch != null) {
            operations.add(Aggregation.match(
                    where(Comment.CONTENT_FIELD).regex(search, "i")
            ));
        }

        var collectionName = mongo.getCollectionName(Post.class);

        //agregamos el ordenamiento
        return MongoUtils.getPage(mongo, pageable, operations, collectionName, Comment.class);
    }


    private static ProjectionOperation projectPost() {
        return Aggregation.project().andInclude(
                Post.CONTENT_FIELD,
                Post.CREATED_AT_FIELD,
                Post.LIKED_BY_FIELD,
                Post.COMMENTS_FIELD
        );
    }

}
