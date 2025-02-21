package ec.gob.conagopare.sona.modules.forum.service;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import ec.gob.conagopare.sona.application.common.schemas.CountResult;
import ec.gob.conagopare.sona.modules.forum.dto.NewComment;
import ec.gob.conagopare.sona.modules.forum.dto.PostDto;
import ec.gob.conagopare.sona.modules.forum.dto.TopPostsResult;
import ec.gob.conagopare.sona.modules.forum.models.Post;
import ec.gob.conagopare.sona.modules.forum.repository.PostRepository;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import ec.gob.conagopare.sona.modules.user.models.User;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.springframework.web.problemdetails.ProblemDetailsException;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestPostService {

    @InjectMocks
    private PostService service;

    @Mock
    private PostRepository repository;

    @Mock
    private MongoTemplate mongo;

    @Mock
    private UserService userService;

    private static User user;

    private static final List<MockedStatic<?>> MOCKED_STATICS = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        var keycloakId = "keycloak-id";
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(keycloakId)
                .build();

        user = User.builder()
                .id(1L)
                .keycloakId(keycloakId)
                .authorities(Set.of(Authority.USER))
                .build();

        var authentication = new JwtAuthenticationToken(jwt);
        var staticMock = Mockito.mockStatic(SecurityContextHolder.class);
        staticMock.when(SecurityContextHolder::getContext).thenReturn(new SecurityContextImpl(authentication));
        MOCKED_STATICS.add(staticMock);

    }

    @AfterAll
    static void tearDown() {
        MOCKED_STATICS.forEach(MockedStatic::close);
    }

    @AfterEach
    void resetMocks() {
        reset(repository, userService);
        user.setAuthorities(Set.of(Authority.USER));
    }

    @Test
    void create_CuandoDatosSonCorrectosModoAnonimo_DebeRetornarPost() {
        // Arrange
        var content = "content";
        var anonymous = false;

        var dto = new PostDto();
        dto.setContent(content);
        dto.setAnonymous(anonymous);

        // Mock
        when(userService.getCurrentUser()).thenReturn(user);
        when(repository.save(any())).then(invocation -> invocation.getArgument(0));

        // Act
        var result = service.create(dto);

        // Assert
        assertNotNull(result, "El resultado no debe ser nulo");
        assertEquals(content, result.getContent(), "El contenido debe ser el mismo");
        assertEquals(anonymous, result.isAnonymous(), "El modo anónimo debe ser el mismo");
    }

    @Test
    void create_CuandoDatosSonCorrectosModoNoAnonimo_DebeRetornarPost() {
        // Arrange
        var content = "content";
        var anonymous = true;

        var dto = new PostDto();
        dto.setContent(content);
        dto.setAnonymous(anonymous);

        // Mock
        when(userService.getCurrentUser()).thenReturn(user);
        when(repository.save(any())).then(invocation -> invocation.getArgument(0));

        // Act
        var result = service.create(dto);

        // Assert
        assertNotNull(result, "El resultado no debe ser nulo");
        assertEquals(content, result.getContent(), "El contenido debe ser el mismo");
        assertEquals(anonymous, result.isAnonymous(), "El modo anónimo debe ser el mismo");
    }

    @Test
    void update_NoSePuedeActualizarPost() {
        // Arrange
        var id = "id";
        var content = "content";
        var anonymous = false;

        var dto = new PostDto();
        dto.setContent(content);
        dto.setAnonymous(anonymous);

        var post = new Post();
        post.setId(id);
        post.setContent("old content");
        post.setAnonymous(true);

        // Mock
        when(repository.findById(id)).thenReturn(Optional.of(post));

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.update(id, dto));
        var body = exception.getBody();
        assertEquals(400, body.getStatus(), "El status debe ser 400");
    }


    @Test
    void delete_CuandoUsuarioEsAutor_DebeEliminarPost() {
        // Arrange
        var id = ObjectId.get().toHexString();

        // Mock
        when(userService.getCurrentUser()).thenReturn(user);
        when(mongo.remove(any(Query.class), eq(Post.class))).thenReturn(DeleteResult.acknowledged(1));

        // Act
        service.delete(id);

        // Assert
        verify(mongo).remove(any(Query.class), eq(Post.class));
    }

    @Test
    void delete_CuandoUsuarioNoEsAutor_DebeLanzarExcepcion() {
        // Arrange
        var id = ObjectId.get().toHexString();

        // Mock
        when(userService.getCurrentUser()).thenReturn(user);
        when(mongo.remove(any(Query.class), eq(Post.class))).thenReturn(DeleteResult.acknowledged(0));

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.delete(id));
        var body = exception.getBody();
        assertEquals(403, body.getStatus(), "El status debe ser 403");
    }


    @Test
    void delete_CuandoUsuarioNoEsAutorPeroEsAdmin_DebeLanzarExcepcion() {
        // Arrange
        var id = ObjectId.get().toHexString();
        user.setAuthorities(Set.of(Authority.ADMIN));

        // Mock
        when(userService.getCurrentUser()).thenReturn(user);
        when(mongo.remove(any(Query.class), eq(Post.class))).thenReturn(DeleteResult.acknowledged(0));

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.delete(id));
        var body = exception.getBody();
        assertEquals(404, body.getStatus(), "El status debe ser 403");
    }

    @Test
    void commentPost_CuandoPostExiste_DebeAgregarComentario() {
        // Arrange
        var id = ObjectId.get().toHexString();
        var anonymous = false;
        var content = "comment";

        var comment = new NewComment();
        comment.setContent(content);
        comment.setAnonymous(anonymous);

        // Mock
        when(userService.getCurrentUser()).thenReturn(user);
        when(mongo.updateFirst(any(Query.class), any(), eq(Post.class))).thenReturn(UpdateResult.acknowledged(1, 1L, null));

        // Act
        service.commentPost(id, comment);

        // Assert
        verify(mongo).updateFirst(any(Query.class), any(), eq(Post.class));
    }

    @Test
    void commentPost_CuandoPostNoExiste_DebeLanzarExcepcion() {
        // Arrange
        var id = ObjectId.get().toHexString();
        var anonymous = false;
        var content = "comment";

        var comment = new NewComment();
        comment.setContent(content);
        comment.setAnonymous(anonymous);

        // Mock
        when(userService.getCurrentUser()).thenReturn(user);
        when(mongo.updateFirst(any(Query.class), any(), eq(Post.class))).thenReturn(UpdateResult.acknowledged(0, 0L, null));

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.commentPost(id, comment));
        var body = exception.getBody();
        assertEquals(404, body.getStatus(), "El status debe ser 404");
    }

    @Test
    void deleteComment_CuandoUsuarioEsAutor_DebeEliminarComentario() {
        // Arrange
        var postId = ObjectId.get().toHexString();
        var commentId = ObjectId.get().toHexString();

        // Mock
        when(userService.getCurrentUser()).thenReturn(user);
        when(mongo.updateFirst(any(Query.class), any(), eq(Post.class))).thenReturn(UpdateResult.acknowledged(1, 1L, null));

        // Act
        service.deleteComment(postId, commentId);

        // Assert
        verify(mongo).updateFirst(any(Query.class), any(), eq(Post.class));
    }


    @Test
    void deleteComment_CuandoUsuarioEsAdmin_DebeEliminarComentario() {
        // Arrange
        var postId = ObjectId.get().toHexString();
        var commentId = ObjectId.get().toHexString();
        user.setAuthorities(Set.of(Authority.ADMIN));

        // Mock
        when(userService.getCurrentUser()).thenReturn(user);
        when(mongo.updateFirst(any(Query.class), any(), eq(Post.class))).thenReturn(UpdateResult.acknowledged(1, 1L, null));

        // Act
        service.deleteComment(postId, commentId);

        // Assert
        verify(mongo).updateFirst(any(Query.class), any(), eq(Post.class));
    }

    @Test
    void likePost_CuandoPostExiste_DebeAgregarLike() {
        // Arrange
        var id = ObjectId.get().toHexString();

        // Mock
        when(userService.getCurrentUser()).thenReturn(user);
        when(mongo.updateFirst(any(Query.class), any(), eq(Post.class))).thenReturn(UpdateResult.acknowledged(1, 1L, null));

        // Act
        service.likePost(id);

        // Assert
        verify(mongo).updateFirst(any(Query.class), any(), eq(Post.class));
    }

    @Test
    void unlikePost_CuandoPostExiste_DebeEliminarLike() {
        // Arrange
        var id = ObjectId.get().toHexString();

        // Mock
        when(userService.getCurrentUser()).thenReturn(user);
        when(mongo.updateFirst(any(Query.class), any(), eq(Post.class))).thenReturn(UpdateResult.acknowledged(1, 1L, null));

        // Act
        service.unlikePost(id);

        // Assert
        verify(mongo).updateFirst(any(Query.class), any(), eq(Post.class));
    }

    @Test
    void reportPost_CuandoPostExiste_DebeReportarPost() {
        // Arrange
        var id = ObjectId.get().toHexString();

        // Mock
        when(userService.getCurrentUser()).thenReturn(user);
        when(mongo.updateFirst(any(Query.class), any(), eq(Post.class))).thenReturn(UpdateResult.acknowledged(1, 1L, null));

        // Act
        service.reportPost(id);

        // Assert
        verify(mongo).updateFirst(any(Query.class), any(), eq(Post.class));
    }

    @Test
    void reportPost_CuandoPostYaFueReportado_DebeLanzarExcepcion() {
        // Arrange
        var id = ObjectId.get().toHexString();

        // Mock
        when(userService.getCurrentUser()).thenReturn(user);
        when(mongo.exists(any(Query.class), eq(Post.class))).thenReturn(true);

        // Act & Assert
        var exception = assertThrows(ProblemDetailsException.class, () -> service.reportPost(id));
        var body = exception.getBody();
        assertEquals(400, body.getStatus(), "El status debe ser 400");
    }

    @Test
    void likeComment_CuandoPostExiste_DebeAgregarLike() {
        // Arrange
        var postId = ObjectId.get().toHexString();
        var commentId = ObjectId.get().toHexString();

        // Mock
        when(userService.getCurrentUser()).thenReturn(user);
        when(mongo.updateFirst(any(Query.class), any(), eq(Post.class))).thenReturn(UpdateResult.acknowledged(1, 1L, null));

        // Act
        service.likeComment(postId, commentId);

        // Assert
        verify(mongo).updateFirst(any(Query.class), any(), eq(Post.class));
    }

    @Test
    void unlikeComment_CuandoPostExiste_DebeEliminarLike() {
        // Arrange
        var postId = ObjectId.get().toHexString();
        var commentId = ObjectId.get().toHexString();

        // Mock
        when(userService.getCurrentUser()).thenReturn(user);
        when(mongo.updateFirst(any(Query.class), any(), eq(Post.class))).thenReturn(UpdateResult.acknowledged(1, 1L, null));

        // Act
        service.unlikeComment(postId, commentId);

        // Assert
        verify(mongo).updateFirst(any(Query.class), any(), eq(Post.class));
    }

    @Test
    void reportComment_CuandoPostExiste_DebeReportarComentario() {
        // Arrange
        var postId = ObjectId.get().toHexString();
        var commentId = ObjectId.get().toHexString();

        // Mock
        when(userService.getCurrentUser()).thenReturn(user);
        when(mongo.updateFirst(any(Query.class), any(), eq(Post.class))).thenReturn(UpdateResult.acknowledged(1, 1L, null));

        // Act
        service.reportComment(postId, commentId);

        // Assert
        verify(mongo).updateFirst(any(Query.class), any(), eq(Post.class));
    }

    @Test
    void page_ConBusquedaFiltrosYPaginacion_DebeRetornarPosts() {
        // Arrange
        var search = "search";
        var pageable = Pageable.ofSize(10).withPage(1);
        var filters = MultiValueMap.fromMultiValue(Map.of(
                "author", List.of("1")
        ));

        var post1 = new Post();
        post1.setAuthor(1L);

        var post2 = new Post();
        post2.setAuthor(2L);
        var posts = List.of(
                post1,
                post2
        );

        // Mock
        when(mongo.getCollectionName(Post.class)).thenReturn("posts");
        when(userService.getCurrentUser()).thenReturn(user);
        when(mongo.aggregate(any(Aggregation.class), any(String.class), eq(CountResult.class))).thenReturn(new AggregationResults<>(List.of(new CountResult(posts.size())), new Document()));
        when(mongo.aggregate(any(Aggregation.class), any(String.class), eq(Post.class))).thenReturn(new AggregationResults<>(posts, new Document()));

        // Act
        var result = service.page(search, pageable, filters);

        // Assert
        assertNotNull(result, "El resultado no debe ser nulo");
        assertEquals(posts.size(), result.getContent().size(), "La cantidad de elementos debe ser la misma");
    }

    @Test
    void page_SinBusquedaSinPaginacionYSinFiltros_DebeRetornarPosts() {
        // Arrange
        var pageable = Pageable.unpaged();
        var filters = new LinkedMultiValueMap<String, String>();

        var post1 = new Post();
        post1.setAuthor(1L);

        var post2 = new Post();
        post2.setAuthor(2L);
        var posts = List.of(
                post1,
                post2
        );

        // Mock
        when(mongo.getCollectionName(Post.class)).thenReturn("posts");
        when(userService.getCurrentUser()).thenReturn(user);
        when(mongo.aggregate(any(Aggregation.class), any(String.class), eq(Post.class))).thenReturn(new AggregationResults<>(posts, new Document()));

        // Act
        var result = service.page(null, pageable, filters);

        // Assert
        assertNotNull(result, "El resultado no debe ser nulo");
        assertEquals(posts.size(), result.getContent().size(), "La cantidad de elementos debe ser la misma");
    }

    @Test
    void topPosts_CuandoSeSolicitaTopPosts_DebeRetornarPosts() {
        // Arrange

        var post = new Post();
        post.setAuthor(1L);

        var topPostsResult = new TopPostsResult();
        topPostsResult.setMostLikedPost(post);
        topPostsResult.setMostCommentedPost(post);

        // Mock
        when(mongo.getCollectionName(Post.class)).thenReturn("posts");
        when(userService.getCurrentUser()).thenReturn(user);
        when(mongo.aggregate(any(Aggregation.class), any(String.class), eq(Post.class))).thenReturn(new AggregationResults<>(List.of(post), new Document()));

        // Act
        var result = service.topPosts();

        // Assert
        assertNotNull(result, "El resultado no debe ser nulo");
        assertNotNull(result.getMostLikedPost(), "El post más gustado no debe ser nulo");
        assertNotNull(result.getMostCommentedPost(), "El post más comentado no debe ser nulo");
    }

    @Test
    void pageComments_ConBusquedaFiltrosYPaginacion_DebeRetornarComentarios() {
        // Arrange
        var postId = ObjectId.get().toHexString();
        var search = "search";
        var pageable = Pageable.ofSize(10).withPage(1);
        var filters = MultiValueMap.fromMultiValue(Map.of(
                "authorId", List.of("1")
        ));

        var comments = new PageImpl<>(List.of(
                new Post.Comment(),
                new Post.Comment()
        ));

        // Mock
        when(mongo.getCollectionName(Post.class)).thenReturn("posts");
        when(userService.getCurrentUser()).thenReturn(user);
        when(mongo.aggregate(any(Aggregation.class), any(String.class), eq(CountResult.class))).thenReturn(new AggregationResults<>(List.of(new CountResult(comments.getTotalElements())), new Document()));
        when(mongo.aggregate(any(Aggregation.class), any(String.class), eq(Post.Comment.class))).thenReturn(new AggregationResults<>(comments.getContent(), new Document()));

        // Act
        var result = service.pageComments(postId, search, pageable, filters);

        // Assert
        assertNotNull(result, "El resultado no debe ser nulo");
    }
}