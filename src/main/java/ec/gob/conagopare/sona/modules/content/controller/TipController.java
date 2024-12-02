package ec.gob.conagopare.sona.modules.content.controller;

import ec.gob.conagopare.sona.application.common.schemas.Message;
import ec.gob.conagopare.sona.application.common.utils.ResponseEntityUtils;
import ec.gob.conagopare.sona.modules.content.dto.TipDto;
import ec.gob.conagopare.sona.modules.content.models.Tip;
import ec.gob.conagopare.sona.modules.content.services.TipService;
import io.github.luidmidev.springframework.data.crud.core.controllers.ReadController;
import io.github.luidmidev.springframework.data.crud.core.utils.PageableUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
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
public class TipController implements ReadController<Tip, UUID, TipService> {

    protected final TipService service;

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


    @GetMapping("/actives")
    public ResponseEntity<List<Tip>> actives(
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(service.actives(search));
    }

    @GetMapping("/actives/page")
    public ResponseEntity<Page<Tip>> activesPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false) String[] properties,
            @RequestParam(required = false) Sort.Direction direction
    ) {
        var pageable = PageableUtils.resolvePage(size, page, direction, properties);
        return ResponseEntity.ok(service.actives(pageable, search));
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
}
