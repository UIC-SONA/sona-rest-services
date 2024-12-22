package ec.gob.conagopare.sona.modules.content.controller;

import ec.gob.conagopare.sona.application.common.utils.ResponseEntityUtils;
import ec.gob.conagopare.sona.modules.content.dto.DidacticContentDto;
import ec.gob.conagopare.sona.modules.content.models.DidacticContent;
import ec.gob.conagopare.sona.modules.content.services.DidacticContentService;
import io.github.luidmidev.springframework.data.crud.core.controllers.ReadController;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Getter
@RestController
@RequiredArgsConstructor
@RequestMapping("/content/didactic")
public class DidacticContentController implements ReadController<DidacticContent, UUID, DidacticContentService> {

    private final DidacticContentService service;

    @PostMapping
    public ResponseEntity<DidacticContent> create(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam MultipartFile image
    ) {
        var dto = new DidacticContentDto(title, content, image);
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DidacticContent> update(
            @PathVariable UUID id,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) MultipartFile image
    ) {
        var dto = new DidacticContentDto(title, content, image);
        return ResponseEntity.ok(service.update(id, dto));
    }

    @GetMapping("/image")
    public ResponseEntity<ByteArrayResource> image(@RequestParam UUID id) throws IOException {
        return ResponseEntityUtils.resource(service.image(id));
    }

}
