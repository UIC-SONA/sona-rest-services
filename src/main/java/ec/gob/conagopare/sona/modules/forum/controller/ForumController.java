package ec.gob.conagopare.sona.modules.forum.controller;

import ec.gob.conagopare.sona.application.common.schemas.Message;
import ec.gob.conagopare.sona.modules.forum.dto.NewComment;
import ec.gob.conagopare.sona.modules.forum.dto.ForumPostDto;
import ec.gob.conagopare.sona.modules.forum.models.Forum;
import ec.gob.conagopare.sona.modules.forum.service.ForumService;
import io.github.luidmidev.springframework.data.crud.core.controllers.CrudController;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Getter
@RestController
@RequestMapping("/forum")
@RequiredArgsConstructor
public class ForumController implements CrudController<Forum, ForumPostDto, String, ForumService> {

    private final ForumService service;

    @PostMapping("/{forumId}/comments")
    public ResponseEntity<Forum.Comment> createComment(
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

    @PostMapping("/{forumId}/like")
    public ResponseEntity<Message> likePost(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String forumId
    ) {
        service.likePost(jwt, forumId);
        return ResponseEntity.ok(new Message("Publicación marcada como favorita"));
    }

    @PostMapping("/{forumId}/unlike")
    public ResponseEntity<Message> unlikePost(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String forumId
    ) {
        service.unlikePost(jwt, forumId);
        return ResponseEntity.ok(new Message("Publicación desmarcada como favorita"));
    }

    @PostMapping("/{forumId}/report")
    public ResponseEntity<Message> reportPost(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String forumId
    ) {
        service.reportPost(jwt, forumId);
        return ResponseEntity.ok(new Message("Publicación reportada correctamente"));
    }

    @PostMapping("/{forumId}/comments/{commentId}/like")
    public ResponseEntity<Message> likeComment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String forumId,
            @PathVariable String commentId
    ) {
        service.likeComment(jwt, forumId, commentId);
        return ResponseEntity.ok(new Message("Comentario marcado como favorito"));
    }

    @PostMapping("/{forumId}/comments/{commentId}/unlike")
    public ResponseEntity<Message> unlikeComment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String forumId,
            @PathVariable String commentId
    ) {
        service.unlikeComment(jwt, forumId, commentId);
        return ResponseEntity.ok(new Message("Comentario desmarcado como favorito"));
    }

    @PostMapping("/{forumId}/comments/{commentId}/report")
    public ResponseEntity<Message> reportComment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String forumId,
            @PathVariable String commentId
    ) {
        service.reportComment(jwt, forumId, commentId);
        return ResponseEntity.ok(new Message("Comentario reportado correctamente"));
    }
}
