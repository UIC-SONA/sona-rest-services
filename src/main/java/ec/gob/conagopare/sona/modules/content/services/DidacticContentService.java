package ec.gob.conagopare.sona.modules.content.services;

import ec.gob.conagopare.sona.application.common.utils.FileUtils;
import ec.gob.conagopare.sona.application.common.utils.StorageUtils;
import ec.gob.conagopare.sona.modules.content.dto.DidacticContentDto;
import ec.gob.conagopare.sona.modules.content.models.DidacticContent;
import ec.gob.conagopare.sona.modules.content.repositories.DidacticContentRepository;
import io.github.luidmidev.springframework.data.crud.jpa.services.JpaCrudService;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import io.github.luidmidev.storage.Storage;
import io.github.luidmidev.storage.Stored;
import jakarta.persistence.EntityManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Getter
public class DidacticContentService implements JpaCrudService<DidacticContent, DidacticContentDto, UUID, DidacticContentRepository> {
    private static final String IMAGES_PATH = "didactic_content";

    private final DidacticContentRepository repository;
    private final EntityManager entityManager;
    private final Storage storage;

    @Override
    public Class<DidacticContent> getEntityClass() {
        return DidacticContent.class;
    }

    @SneakyThrows
    @Override
    public void mapModel(DidacticContentDto dto, DidacticContent model) {
        if (model.isNew()) {
            var image = dto.getImage();
            if (image == null) {
                throw ApiError.badRequest("La imagen es requerida");
            }

            var imagePath = storage.store(image.getBytes(), FileUtils.factoryDateTimeFileName(image.getOriginalFilename()), IMAGES_PATH);
            model.setImage(imagePath);

        } else {
            var image = dto.getImage();
            if (image != null) {
                var oldImage = model.getImage();
                var imagePath = storage.store(image.getBytes(), FileUtils.factoryDateTimeFileName(image.getOriginalFilename()), IMAGES_PATH);
                model.setImage(imagePath);

                StorageUtils.tryRemoveFileAsync(storage, oldImage);
            }
        }

        model.setTitle(dto.getTitle());
        model.setContent(dto.getContent());
    }

    @Override
    public Page<DidacticContent> internalSearch(String search, Pageable pageable, MultiValueMap<String, String> params) {
        throw ApiError.badRequest("Filtro no soportado");
    }

    @PreAuthorize("isAuthenticated()")
    public Stored image(UUID id) throws IOException {
        var model = find(id);
        return storage
                .download(model.getImage())
                .orElseThrow(() -> ApiError.notFound("Imagen no encontrada"));
    }
}
