package ec.gob.conagopare.sona.modules.forum.service;

import ec.gob.conagopare.sona.modules.forum.dto.NewComment;
import ec.gob.conagopare.sona.modules.forum.dto.ForumPostDto;
import ec.gob.conagopare.sona.modules.forum.models.ByAuthor;
import ec.gob.conagopare.sona.modules.forum.models.Forum;
import ec.gob.conagopare.sona.modules.forum.models.Forum.Comment;
import ec.gob.conagopare.sona.modules.forum.repository.ForumRepository;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import ec.gob.conagopare.sona.modules.user.models.User;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.springframework.data.crud.core.filters.Filter;
import io.github.luidmidev.springframework.data.crud.core.filters.FilterOperator;
import io.github.luidmidev.springframework.data.crud.core.filters.FilterProcessor;
import io.github.luidmidev.springframework.data.crud.core.filters.FilterProcessor.FilterMatcher;
import io.github.luidmidev.springframework.data.crud.core.services.CrudService;
import io.github.luidmidev.springframework.data.crud.core.utils.StringUtils;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Slf4j
@Service
@Transactional
public class ForumService extends CrudService<Forum, ForumPostDto, String, ForumRepository> {

    private static final Set<Authority> PRIVILEGED_AUTHORITIES = Set.of(Authority.ADMIN, Authority.ADMINISTRATIVE);

    private final MongoTemplate mongo;
    private final UserService userService;

    public ForumService(MongoTemplate mongo, UserService userService, ForumRepository repository) {
        super(repository, Forum.class);
        this.mongo = mongo;
        this.userService = userService;
    }


    @Override
    @SneakyThrows
    protected void mapModel(ForumPostDto dto, Forum model) {
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
    public Page<Forum> myLikedPosts(Jwt jwt, Pageable pageable) {
        var user = userService.getUser(jwt);
        var query = Query.query(where(Forum.LIKED_BY_FIELD).is(user.getId()));
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
            var comment = Forum.newComment(content, user.getId(), anonymous);
            update.push(Forum.COMMENT_FIELD, comment);
            return comment;
        });
    }

    @PreAuthorize("isAuthenticated()")
    public void deleteComment(Jwt jwt, String postId, String commentId) {
        var user = userService.getUser(jwt);

        var criteria = where("id").is(postId);

        if (isPriviliged(user)) {
            criteria.and(Forum.COMMENT_FIELD).elemMatch(where("id").is(commentId));
        } else {
            criteria.andOperator(
                    new Criteria().orOperator(
                            where(Forum.COMMENT_FIELD).elemMatch(where("id").is(commentId).and(ByAuthor.AUTHOR_FIELD).is(user.getId())),
                            where(ByAuthor.AUTHOR_FIELD).is(user.getId())
                    )
            );
        }

        var query = Query.query(criteria);
        updatePost(query, update -> update.pull(Forum.COMMENT_FIELD, Query.query(where("id").is(commentId))));
    }

    @PreAuthorize("isAuthenticated()")
    public void likePost(Jwt jwt, String postId) {
        updatePost(jwt, isId(postId), (update, user) -> update.addToSet(Forum.LIKED_BY_FIELD, user.getId()));
    }

    @PreAuthorize("isAuthenticated()")
    public void unlikePost(Jwt jwt, String postId) {
        updatePost(jwt, isId(postId), (update, user) -> update.pull(Forum.LIKED_BY_FIELD, user.getId()));
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
        var result = mongo.updateFirst(query, update, Forum.class);
        if (result.getModifiedCount() == 0) {
            log.warn("No se encontró la publicación para actualizar");
        }
        return returned;
    }


    private void deletePost(Query query) {
        var result = mongo.remove(query, Forum.class);
        if (result.getDeletedCount() == 0) {
            log.warn("No se encontró la publicación para eliminar");
        }
    }

    @Override
    protected Page<Forum> search(String search, Pageable pageable) {
        return repository.findAllByContentContainingIgnoreCase(search, pageable);
    }

    @Override
    protected Page<Forum> search(String search, Pageable pageable, Filter filter) {
        return FilterProcessor.process(
                filter,
                () -> {
                    throw ApiError.badRequest("Filtro no soportado");
                },
                FilterProcessor
                        .of(new FilterMatcher("author", FilterOperator.EQ))
                        .resolve(values -> {
                            var author = (Long) values[0];
                            if (!StringUtils.isBlank(search)) {
                                return repository.findAllByAuthorAndContentContainingIgnoreCase(author, search, pageable);
                            }
                            return repository.findAllByAuthor(author, pageable);
                        })
        );
    }

    private Page<Forum> paginatePost(Pageable pageable, Query query) {
        var pagedQuery = Query.of(query).with(pageable);

        return PageableExecutionUtils.getPage(
                mongo.find(pagedQuery, Forum.class),
                pageable,
                () -> mongo.count(query, Forum.class)
        );
    }


    public void likeComment(Jwt jwt, String forumId, String commentId) {
        updatePost(jwt, isId(forumId), (update, user) -> update
                .addToSet(Forum.COMMENT_FIELD + ".$[comment]." + Forum.Comment.LIKED_BY_FIELD, user.getId())
                .filterArray(where("comment._id").is(commentId))
        );
    }

    public void unlikeComment(Jwt jwt, String forumId, String commentId) {
        updatePost(jwt, isId(forumId), (update, user) -> update
                .pull(Forum.COMMENT_FIELD + ".$[comment]." + Forum.Comment.LIKED_BY_FIELD, user.getId())
                .filterArray(where("comment._id").is(commentId))
        );
    }

    public void reportComment(Jwt jwt, String forumId, String commentId) {
        updatePost(jwt, isId(forumId), (update, user) -> update
                .addToSet(Forum.COMMENT_FIELD + ".$[comment]." + Forum.Comment.REPORTED_BY_FIELD, user.getId())
                .filterArray(where("comment._id").is(commentId))
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
