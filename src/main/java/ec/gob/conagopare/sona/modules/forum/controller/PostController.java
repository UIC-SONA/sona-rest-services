package ec.gob.conagopare.sona.modules.forum.controller;

import ec.gob.conagopare.sona.application.common.schemas.Message;
import ec.gob.conagopare.sona.modules.forum.dto.NewComment;
import ec.gob.conagopare.sona.modules.forum.dto.PostDto;
import ec.gob.conagopare.sona.modules.forum.dto.TopPostsDto;
import ec.gob.conagopare.sona.modules.forum.models.Post;
import ec.gob.conagopare.sona.modules.forum.service.PostService;
import io.github.luidmidev.springframework.data.crud.core.controllers.CrudController;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Getter
@RestController
@RequestMapping("/forum/post")
@RequiredArgsConstructor
public class PostController implements CrudController<Post, PostDto, String, PostService> {

    private final PostService service;

    @PostMapping("/{forumId}/comments")
    public ResponseEntity<Post.Comment> createComment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String forumId,
            @RequestBody NewComment newComment
    ) {
        var comment = service.commentPost(jwt, forumId, newComment);
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/{forumId}/comments/{commentId}")
    public ResponseEntity<Message> deleteComment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String forumId,
            @PathVariable String commentId
    ) {
        service.deleteComment(jwt, forumId, commentId);
        return ResponseEntity.ok(new Message("Comentario eliminado correctamente"));
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<Message> likePost(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String postId
    ) {
        service.likePost(jwt, postId);
        return ResponseEntity.ok(new Message("Publicación marcada como favorita"));
    }

    @PostMapping("/{postId}/unlike")
    public ResponseEntity<Message> unlikePost(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String postId
    ) {
        service.unlikePost(jwt, postId);
        return ResponseEntity.ok(new Message("Publicación desmarcada como favorita"));
    }

    @PostMapping("/{postId}/report")
    public ResponseEntity<Message> reportPost(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String postId
    ) {
        service.reportPost(jwt, postId);
        return ResponseEntity.ok(new Message("Publicación reportada correctamente"));
    }

    @PostMapping("/{postId}/comments/{commentId}/like")
    public ResponseEntity<Message> likeComment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String postId,
            @PathVariable String commentId
    ) {
        service.likeComment(jwt, postId, commentId);
        return ResponseEntity.ok(new Message("Comentario marcado como favorito"));
    }

    @PostMapping("/{postId}/comments/{commentId}/unlike")
    public ResponseEntity<Message> unlikeComment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String postId,
            @PathVariable String commentId
    ) {
        service.unlikeComment(jwt, postId, commentId);
        return ResponseEntity.ok(new Message("Comentario desmarcado como favorito"));
    }

    @PostMapping("/{postId}/comments/{commentId}/report")
    public ResponseEntity<Message> reportComment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String postId,
            @PathVariable String commentId
    ) {
        service.reportComment(jwt, postId, commentId);
        return ResponseEntity.ok(new Message("Comentario reportado correctamente"));
    }

    @GetMapping("/top")
    public ResponseEntity<TopPostsDto> topPosts() {
        return ResponseEntity.ok(service.topPosts());
    }
}
