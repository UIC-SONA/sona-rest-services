package ec.gob.conagopare.sona.modules.content.controller;

import ec.gob.conagopare.sona.application.common.utils.ResponseEntityUtils;
import ec.gob.conagopare.sona.modules.content.dto.TipDto;
import ec.gob.conagopare.sona.modules.content.models.Tip;
import ec.gob.conagopare.sona.modules.content.services.TipService;
import io.github.luidmidev.springframework.data.crud.core.CRUDMessagesResolver;
import io.github.luidmidev.springframework.data.crud.core.utils.PageableUtils;
import lombok.Getter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/content/tips")
public class TipController {

    @Getter
    protected final TipService service;
    protected final CRUDMessagesResolver<UUID> messagesResolver;

    protected TipController(TipService service) {
        this.service = service;
        this.messagesResolver = CRUDMessagesResolver.<UUID>builder()
                .deleted(() -> "Tip eliminado correctamente")
                .notFound(id -> "Tip no encontrado: " + id)
                .build();

        this.service.setNotFoundMessageResolver(messagesResolver.getNotFound());
    }

    @PreAuthorize("hasRole('admin')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Tip> create(
            @RequestParam String title,
            @RequestParam String summary,
            @RequestParam boolean active,
            @RequestParam List<String> tags,
            @RequestPart String description,
            @RequestPart MultipartFile image
    ) {
        var dto = new TipDto(title, summary, description, tags, active, image);
        var model = service.create(dto);
        return ResponseEntity.ok(model);
    }

    @PreAuthorize("hasRole('admin')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Tip> update(
            @PathVariable("id") UUID id,
            @RequestParam String title,
            @RequestParam String summary,
            @RequestParam boolean active,
            @RequestParam List<String> tags,
            @RequestPart String description,
            @RequestPart(required = false) MultipartFile image
    ) {
        var dto = new TipDto(title, summary, description, tags, active, image);
        var model = service.update(id, dto);
        return ResponseEntity.ok(model);
    }

    @PreAuthorize("hasRole('admin')")
    @GetMapping
    public ResponseEntity<Page<Tip>> page(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "properties", required = false) List<String> properties,
            @RequestParam(name = "direction", required = false) Sort.Direction direction
    ) {
        var pageable = PageableUtils.resolvePage(size, page, direction, properties);
        return ResponseEntity.ok(search == null
                ? service.page(pageable)
                : service.search(search, pageable)
        );
    }

    @PreAuthorize("hasRole('admin')")
    @GetMapping("/all")
    public ResponseEntity<List<Tip>> list(
            @RequestParam(name = "search", required = false) String search
    ) {
        return ResponseEntity.ok(search == null
                ? service.list()
                : service.search(search)
        );
    }

    @PreAuthorize("hasRole('admin')")
    @GetMapping("/{id}")
    public ResponseEntity<Tip> find(@PathVariable("id") UUID id) {
        var model = service.find(id);
        return ResponseEntity.ok(model);
    }

    @PreAuthorize("hasRole('admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable("id") UUID id) {
        service.delete(id);
        return ResponseEntity.ok(messagesResolver.getDeleted().get());
    }

    @PreAuthorize("hasRole('admin')")
    @PostMapping("/exists")
    public ResponseEntity<Boolean> exists(@RequestBody UUID id) {
        return ResponseEntity.ok(service.exists(id));
    }

    @PreAuthorize("hasRole('admin')")
    @PostMapping("/exists-all")
    public ResponseEntity<Boolean> existsAll(@RequestBody List<UUID> ids) {
        return ResponseEntity.ok(service.existsAll(ids));
    }


    @PreAuthorize("isAuthenticated()")
    @GetMapping("/active")
    public ResponseEntity<List<Tip>> active() {
        return ResponseEntity.ok(service.active());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/image")
    public ResponseEntity<ByteArrayResource> image(@PathVariable("id") UUID id) throws IOException {
        return ResponseEntityUtils.resource(service.image(id));
    }

    @PreAuthorize("hasRole('admin')")
    @DeleteMapping("/{id}/image")
    public ResponseEntity<String> deleteImage(@PathVariable("id") UUID id) {
        service.deleteImage(id);
        return ResponseEntity.ok("Imagen eliminada correctamente");
    }
}
