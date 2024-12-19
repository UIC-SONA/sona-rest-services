package ec.gob.conagopare.sona.modules.forum.controller;

import ec.gob.conagopare.sona.application.common.schemas.Message;
import ec.gob.conagopare.sona.modules.forum.dto.NewComment;
import ec.gob.conagopare.sona.modules.forum.dto.PostDto;
import ec.gob.conagopare.sona.modules.forum.models.Post;
import ec.gob.conagopare.sona.modules.forum.service.PostService;
import io.github.luidmidev.springframework.data.crud.core.controllers.ReadController;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@RestController
@RequestMapping("/forum/post")
@RequiredArgsConstructor
public class PostController implements ReadController<Post, String, PostService> {

    private final PostService service;

    @PostMapping("/{postId}/comments")
    public ResponseEntity<Post.Comment> createComment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String postId,
            @RequestBody NewComment newComment
    ) {
        var comment = service.commentPost(jwt, postId, newComment);
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Message> deleteComment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String postId,
            @PathVariable String commentId
    ) {
        service.deleteComment(jwt, postId, commentId);
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Post> create(
            @RequestParam(required = false) Boolean anonymous,
            @RequestParam String content,
            @RequestParam(required = false) List<MultipartFile> images
    ) {
        var post = service.create(new PostDto(anonymous, content, images));
        return ResponseEntity.ok(post);
    }
}
