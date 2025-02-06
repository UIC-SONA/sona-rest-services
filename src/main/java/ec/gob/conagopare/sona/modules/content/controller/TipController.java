package ec.gob.conagopare.sona.modules.content.controller;

import ec.gob.conagopare.sona.application.common.schemas.Message;
import ec.gob.conagopare.sona.application.common.utils.ResponseEntityUtils;
import ec.gob.conagopare.sona.modules.content.dto.TipDto;
import ec.gob.conagopare.sona.modules.content.models.Tip;
import ec.gob.conagopare.sona.modules.content.services.TipService;
import io.github.luidmidev.springframework.data.crud.core.http.controllers.ExportController;
import io.github.luidmidev.springframework.data.crud.core.http.controllers.ReadController;
import io.github.luidmidev.springframework.data.crud.core.http.export.SpreadSheetExporter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;


@Getter
@RestController
@RequestMapping("/content/tips")
@RequiredArgsConstructor
public class TipController implements ReadController<Tip, UUID, TipService>, ExportController<UUID, TipService> {

    private final TipService service;
    private final SpreadSheetExporter exporter;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Tip> create(
            @RequestPart String title,
            @RequestPart String summary,
            @RequestPart List<String> tags,
            @RequestPart boolean active,
            @RequestPart String description,
            @RequestPart MultipartFile image
    ) {
        var dto = new TipDto(title, summary, description, tags, active, image);
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Tip> update(
            @PathVariable UUID id,
            @RequestPart String title,
            @RequestPart String summary,
            @RequestPart boolean active,
            @RequestPart List<String> tags,
            @RequestPart String description,
            @RequestPart(required = false) MultipartFile image
    ) {
        var dto = new TipDto(title, summary, description, tags, active, image);
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Message> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(new Message("Tip eliminado correctamente"));
    }

    @GetMapping("/actives")
    public ResponseEntity<Page<Tip>> activesPage(
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        return ResponseEntity.ok(service.actives(search, pageable));
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<ByteArrayResource> image(@PathVariable("id") UUID id) throws IOException {
        return ResponseEntityUtils.resource(service.image(id));
    }

    @DeleteMapping("/{id}/image")
    public ResponseEntity<Message> deleteImage(@PathVariable("id") UUID id) {
        service.deleteImage(id);
        return ResponseEntity.ok(new Message("Imagen eliminada correctamente"));
    }

    @PostMapping("/rate/{id}")
    public ResponseEntity<Message> rate(@PathVariable("id") UUID id, @RequestParam int value) {
        service.rate(id, value);
        return ResponseEntity.ok(new Message("Valoración realizada correctamente"));
    }

    @GetMapping("/top")
    public ResponseEntity<List<Tip>> top() {
        return ResponseEntity.ok(service.top());
    }

}
