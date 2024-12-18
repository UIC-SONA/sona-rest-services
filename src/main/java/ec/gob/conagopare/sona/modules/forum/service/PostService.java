package ec.gob.conagopare.sona.modules.forum.service;

import ec.gob.conagopare.sona.application.common.utils.FileUtils;
import ec.gob.conagopare.sona.application.common.utils.StorageUtils;
import ec.gob.conagopare.sona.modules.forum.dto.NewComment;
import ec.gob.conagopare.sona.modules.forum.dto.PostDto;
import ec.gob.conagopare.sona.modules.forum.models.ByAuthor;
import ec.gob.conagopare.sona.modules.forum.models.Post;
import ec.gob.conagopare.sona.modules.forum.models.Post.Comment;
import ec.gob.conagopare.sona.modules.forum.repository.PostRepository;
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
import io.github.luidmidev.storage.Storage;
import io.github.luidmidev.storage.ToStore;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static ec.gob.conagopare.sona.application.common.utils.functions.FunctionThrowable.unchecked;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Slf4j
@Service
@Transactional
public class PostService extends CrudService<Post, PostDto, String, PostRepository> {

    private static final Set<Authority> PRIVILEGED_AUTHORITIES = Set.of(Authority.ADMIN, Authority.ADMINISTRATIVE);
    private static final String USERS_POST_PATH = "users/%d/posts";

    private final MongoTemplate mongo;
    private final UserService userService;
    private final Storage storage;

    public PostService(MongoTemplate mongo, UserService userService, Storage storage, PostRepository repository) {
        super(repository, Post.class);
        this.mongo = mongo;
        this.userService = userService;
        this.storage = storage;
    }


    @Override
    @SneakyThrows
    protected void mapModel(PostDto dto, Post model) {
        var jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var user = userService.getUser(jwt);

        if (!model.isNew()) {
            throw ApiError.badRequest("Las publicaciones no pueden ser modificadas");
        }

        var images = dto.getImages();
        var content = dto.getContent();
        var isAnonymous = user.isAnonymous() || Boolean.TRUE.equals(dto.getAnonymous());

        var paths = storePostImages(images, user.getId());

        try {

            model.setContent(content);
            model.setImages(paths);
            model.setCreatedAt(Instant.now());
            model.setAuthor(user.getId());
            model.setAnonymous(isAnonymous);

        } catch (Exception e) {
            StorageUtils.tryRemoveFileAsync(storage, paths);
            throw e;
        }
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
            var anonymous = user.isAnonymous() || Boolean.TRUE.equals(newComment.getAnonymous());
            var comment = Post.newComment(content, user.getId(), anonymous);
            update.push(Post.COMMENT_FIELD, comment);
            return comment;
        });
    }

    @PreAuthorize("isAuthenticated()")
    public void deleteComment(Jwt jwt, String postId, String commentId) {
        var user = userService.getUser(jwt);

        var criteria = where("id").is(postId);

        if (isPriviliged(user)) {
            criteria.and(Post.COMMENT_FIELD).elemMatch(where("id").is(commentId));
        } else {
            criteria.andOperator(
                    new Criteria().orOperator(
                            where(Post.COMMENT_FIELD).elemMatch(where("id").is(commentId).and(ByAuthor.AUTHOR_FIELD).is(user.getId())),
                            where(ByAuthor.AUTHOR_FIELD).is(user.getId())
                    )
            );
        }

        var query = Query.query(criteria);
        updatePost(query, update -> update.pull(Post.COMMENT_FIELD, Query.query(where("id").is(commentId))));
    }

    @PreAuthorize("isAuthenticated()")
    public void likePost(Jwt jwt, String postId) {
        updatePost(jwt, isId(postId), (update, user) -> update.addToSet(Post.LIKED_BY_FIELD, user.getId()));
    }

    @PreAuthorize("isAuthenticated()")
    public void unlikePost(Jwt jwt, String postId) {
        updatePost(jwt, isId(postId), (update, user) -> update.pull(Post.LIKED_BY_FIELD, user.getId()));
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
        query.fields().include("images");
        var findPost = getPost(query);
        mongo.remove(findPost);
        StorageUtils.purgeAsync(storage, findPost);
    }

    public Post getPost(Query query) {
        var post = mongo.findOne(query, Post.class);
        if (post == null) throw ApiError.notFound("No se encontró la publicación");
        return post;
    }

    @Override
    protected Iterable<Post> search(String search, Sort sort) {
        return repository.findAllByContentContainingIgnoreCase(search, sort);
    }

    @Override
    protected Page<Post> search(String search, Pageable pageable) {
        return repository.findAllByContentContainingIgnoreCase(search, pageable);
    }

    @Override
    protected Iterable<Post> search(String search, Sort sort, Filter filter) {
        return FilterProcessor.process(filter,
                () -> search(search, sort),
                FilterProcessor.of(new FilterMatcher("author", FilterOperator.EQ))
                        .resolve(values -> {
                            var author = (Long) values[0];
                            if (StringUtils.isNullOrEmpty(search)) {
                                return repository.findAllByAuthor(author, sort);
                            }
                            return repository.findAllByAuthorAndContentContainingIgnoreCase(author, search, sort);
                        })
        );
    }

    @Override
    protected Page<Post> search(String search, Pageable pageable, Filter filter) {
        return FilterProcessor.process(filter,
                () -> search(search, pageable),
                FilterProcessor.of(new FilterMatcher("author", FilterOperator.EQ))
                        .resolve(values -> {
                            var author = (Long) values[0];
                            if (!StringUtils.isNullOrEmpty(search)) {
                                return repository.findAllByAuthorAndContentContainingIgnoreCase(author, search, pageable);
                            }
                            return repository.findAllByAuthor(author, pageable);
                        })
        );
    }

    private Page<Post> paginatePost(Pageable pageable, Query query) {
        var pagedQuery = Query.of(query).with(pageable);

        return PageableExecutionUtils.getPage(
                mongo.find(pagedQuery, Post.class),
                pageable,
                () -> mongo.count(query, Post.class)
        );
    }

    private List<String> storePostImages(List<MultipartFile> images, Long userId) throws IOException {
        var path = String.format(USERS_POST_PATH, userId);
        var toStore = images.stream()
                .map(unchecked(
                        image -> new ToStore(
                                path,
                                FileUtils.factoryDateTimeFileName(image.getOriginalFilename()),
                                image.getBytes()
                        ))
                ).toArray(ToStore[]::new);
        storage.store(toStore);
        return Stream.of(toStore).map(ToStore::getCompletePath).toList();
    }

    private static boolean isPriviliged(User user) {
        return user.getAuthorities().stream().anyMatch(PRIVILEGED_AUTHORITIES::contains);
    }

    private static Query isAuthor(String id, Long author) {
        return Query.query(where("id").is(id).and(ByAuthor.AUTHOR_FIELD).is(author));
    }

    private static Query isId(String id) {
        return Query.query(where("id").is(id));
    }
}
