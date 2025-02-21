package ec.gob.conagopare.sona.modules.forum.controller;

import ec.gob.conagopare.sona.application.common.schemas.Message;
import ec.gob.conagopare.sona.modules.forum.dto.NewComment;
import ec.gob.conagopare.sona.modules.forum.dto.PostDto;
import ec.gob.conagopare.sona.modules.forum.dto.TopPostsResult;
import ec.gob.conagopare.sona.modules.forum.models.Post;
import ec.gob.conagopare.sona.modules.forum.service.PostService;
import io.github.luidmidev.springframework.data.crud.core.SpringDataCrudAutoConfiguration;
import io.github.luidmidev.springframework.data.crud.core.http.controllers.CrudController;
import io.github.luidmidev.springframework.data.crud.core.http.controllers.ExportController;
import io.github.luidmidev.springframework.data.crud.core.http.export.SpreadSheetExporter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@Getter
@RestController
@RequestMapping("/forum/post")
@RequiredArgsConstructor
public class PostController implements CrudController<Post, PostDto, String, PostService>, ExportController<String, PostService> {

    private final PostService service;
    private final SpreadSheetExporter exporter;

    @PostMapping("/{postId}/like")
    public ResponseEntity<Message> likePost(
            @PathVariable String postId
    ) {
        service.likePost(postId);
        return ResponseEntity.ok(new Message("Publicación marcada como favorita"));
    }

    @PostMapping("/{postId}/unlike")
    public ResponseEntity<Message> unlikePost(
            @PathVariable String postId
    ) {
        service.unlikePost(postId);
        return ResponseEntity.ok(new Message("Publicación desmarcada como favorita"));
    }

    @PostMapping("/{postId}/report")
    public ResponseEntity<Message> reportPost(
            @PathVariable String postId
    ) {
        service.reportPost(postId);
        return ResponseEntity.ok(new Message("Publicación reportada correctamente"));
    }

    @GetMapping("/top")
    public ResponseEntity<TopPostsResult> topPosts() {
        return ResponseEntity.ok(service.topPosts());
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<Post.Comment> createComment(
            @PathVariable String postId,
            @RequestBody NewComment newComment
    ) {
        var comment = service.commentPost(postId, newComment);
        return ResponseEntity.ok(comment);
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<Page<Post.Comment>> pageComments(
            @PathVariable(required = false) String postId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) MultiValueMap<String, String> filters,
            Pageable pageable
    ) {
        var ignoreParams = SpringDataCrudAutoConfiguration.getIgnoreParams();
        if (ignoreParams != null) ignoreParams.forEach(filters::remove);
        return ResponseEntity.ok(service.pageComments(postId, search, pageable, filters));
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Message> deleteComment(
            @PathVariable String postId,
            @PathVariable String commentId
    ) {
        service.deleteComment(postId, commentId);
        return ResponseEntity.ok(new Message("Comentario eliminado correctamente"));
    }


    @PostMapping("/{postId}/comments/{commentId}/like")
    public ResponseEntity<Message> likeComment(
            @PathVariable String postId,
            @PathVariable String commentId
    ) {
        service.likeComment(postId, commentId);
        return ResponseEntity.ok(new Message("Comentario marcado como favorito"));
    }

    @PostMapping("/{postId}/comments/{commentId}/unlike")
    public ResponseEntity<Message> unlikeComment(
            @PathVariable String postId,
            @PathVariable String commentId
    ) {
        service.unlikeComment(postId, commentId);
        return ResponseEntity.ok(new Message("Comentario desmarcado como favorito"));
    }

    @PostMapping("/{postId}/comments/{commentId}/report")
    public ResponseEntity<Message> reportComment(
            @PathVariable String postId,
            @PathVariable String commentId
    ) {
        service.reportComment(postId, commentId);
        return ResponseEntity.ok(new Message("Comentario reportado correctamente"));
    }
}
