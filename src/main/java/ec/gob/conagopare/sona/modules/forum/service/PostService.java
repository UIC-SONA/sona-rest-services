package ec.gob.conagopare.sona.modules.forum.service;

import ec.gob.conagopare.sona.modules.forum.dto.NewComment;
import ec.gob.conagopare.sona.modules.forum.dto.PostDto;
import ec.gob.conagopare.sona.modules.forum.models.ByAuthor;
import ec.gob.conagopare.sona.modules.forum.models.Post;
import ec.gob.conagopare.sona.modules.forum.models.Post.Comment;
import ec.gob.conagopare.sona.modules.forum.repository.PostRepository;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import ec.gob.conagopare.sona.modules.user.models.User;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.springframework.data.crud.core.services.CrudService;
import io.github.luidmidev.springframework.data.crud.core.utils.StringUtils;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import jakarta.persistence.EntityManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.time.Instant;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Slf4j
@Service
@Getter
@RequiredArgsConstructor
public class PostService implements CrudService<Post, PostDto, String, PostRepository> {

    private static final Set<Authority> PRIVILEGED_AUTHORITIES = Set.of(Authority.ADMIN, Authority.ADMINISTRATIVE);
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
        var jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var user = userService.getUser(jwt);

        if (!model.isNew()) {
            throw ApiError.badRequest("Las publicaciones no pueden ser modificadas");
        }

        var content = dto.getContent();
        var isAnonymous = solveAnonymous(user, dto.getAnonymous());

        model.setContent(content);
        model.setCreatedAt(Instant.now());
        model.setAuthor(user.getId());
        model.setAnonymous(isAnonymous);
    }

    @PreAuthorize("isAuthenticated()")
    public Page<Post> myLikedPosts(Jwt jwt, Pageable pageable) {
        var user = userService.getUser(jwt);
        var query = Query.query(where(Post.LIKED_BY_FIELD).is(user.getId()));
        return paginatePost(pageable, query);
    }

    @Override
    public void delete(String id) {
        var jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var user = userService.getUser(jwt);
        var query = isPriviliged(user) ? isId(id) : isAuthor(id, user.getId());
        deletePost(query);
    }

    @PreAuthorize("isAuthenticated()")
    public Comment commentPost(Jwt jwt, String postId, NewComment newComment) {
        return updatePost(jwt, isId(postId), (update, user) -> {
            var content = newComment.getContent();
            var anonymous = solveAnonymous(user, newComment.getAnonymous());
            var comment = Post.newComment(content, user.getId(), anonymous);
            update.push(Post.COMMENTS_FIELD, comment);
            return comment;
        });
    }

    @PreAuthorize("isAuthenticated()")
    public void deleteComment(Jwt jwt, String postId, String commentId) {
        var user = userService.getUser(jwt);

        var criteria = where("id").is(postId);

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
    public void likePost(Jwt jwt, String postId) {
        updatePost(jwt, isId(postId), (update, user) -> update.addToSet(Post.LIKED_BY_FIELD, user.getId()));
    }

    @PreAuthorize("isAuthenticated()")
    public void unlikePost(Jwt jwt, String postId) {
        updatePost(jwt, isId(postId), (update, user) -> update.pull(Post.LIKED_BY_FIELD, user.getId()));
    }

    @PreAuthorize("isAuthenticated()")
    public void reportPost(Jwt jwt, String postId) {
        updatePost(jwt, isId(postId), (update, user) -> update.addToSet("reportedBy", user.getId()));
    }

    private <T> T updatePost(Jwt jwt, Query query, BiFunction<Update, User, T> updater) {
        var user = userService.getUser(jwt);
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


    private void deletePost(Query query) {
        var result = mongo.remove(query, Post.class);
        if (result.getDeletedCount() == 0) {
            log.warn("No se encontró la publicación para eliminar");
        }
    }

    @Override
    public Page<Post> internalSearch(String search, Pageable pageable) {
        return repository.findAllByContentContainingIgnoreCase(search, pageable);
    }

    @Override
    public Page<Post> internalSearch(String search, Pageable pageable, MultiValueMap<String, String> params) {

        var author = params.getFirst("author");
        if (author != null) {
            var authorId = Long.parseLong(author);
            return StringUtils.isBlank(search)
                    ? repository.findAllByAuthor(authorId, pageable)
                    : repository.findAllByAuthorAndContentContainingIgnoreCase(authorId, search, pageable);
        }

        throw ApiError.badRequest("Filtro no soportado");
    }

    private Page<Post> paginatePost(Pageable pageable, Query query) {
        var pagedQuery = Query.of(query).with(pageable);

        return PageableExecutionUtils.getPage(
                mongo.find(pagedQuery, Post.class),
                pageable,
                () -> mongo.count(query, Post.class)
        );
    }


    public void likeComment(Jwt jwt, String forumId, String commentId) {

        updatePost(jwt, isId(forumId), (update, user) -> update
                .addToSet(Post.COMMENTS_FIELD + ".$[" + COMMENT_ARRAY_FILTER + "]." + Post.Comment.LIKED_BY_FIELD, user.getId())
                .filterArray(where(COMMENT_ID_FILTER).is(commentId))
        );
    }

    public void unlikeComment(Jwt jwt, String forumId, String commentId) {
        updatePost(jwt, isId(forumId), (update, user) -> update
                .pull(Post.COMMENTS_FIELD + ".$[" + COMMENT_ARRAY_FILTER + "]." + Post.Comment.LIKED_BY_FIELD, user.getId())
                .filterArray(where(COMMENT_ID_FILTER).is(commentId))
        );
    }

    public void reportComment(Jwt jwt, String forumId, String commentId) {
        updatePost(jwt, isId(forumId), (update, user) -> update
                .addToSet(Post.COMMENTS_FIELD + ".$[" + COMMENT_ARRAY_FILTER + "]." + Post.Comment.REPORTED_BY_FIELD, user.getId())
                .filterArray(where(COMMENT_ID_FILTER).is(commentId))
        );
    }

    private static boolean solveAnonymous(User user, Boolean anonymous) {
        if (anonymous == null) {
            return user.isAnonymous();
        }
        return anonymous;
    }

    private static boolean isPriviliged(User user) {
        return user
                .getAuthorities()
                .stream()
                .anyMatch(PRIVILEGED_AUTHORITIES::contains);
    }

    private static Query isAuthor(String id, Long author) {
        return Query.query(where("id").is(id).and(ByAuthor.AUTHOR_FIELD).is(author));
    }

    private static Query isId(String id) {
        return Query.query(where("id").is(id));
    }

}
