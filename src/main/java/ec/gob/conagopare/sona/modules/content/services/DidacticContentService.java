package ec.gob.conagopare.sona.modules.content.services;

import ec.gob.conagopare.sona.application.common.utils.FileUtils;
import ec.gob.conagopare.sona.application.common.utils.StorageUtils;
import ec.gob.conagopare.sona.modules.content.dto.DidacticContentDto;
import ec.gob.conagopare.sona.modules.content.models.DidacticContent;
import ec.gob.conagopare.sona.modules.content.repositories.DidacticContentRepository;
import io.github.luidmidev.springframework.data.crud.core.filters.Filter;
import io.github.luidmidev.springframework.data.crud.jpa.services.JpaCrudService;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import io.github.luidmidev.storage.Storage;
import io.github.luidmidev.storage.Stored;
import jakarta.persistence.EntityManager;
import lombok.SneakyThrows;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

@Service
public class DidacticContentService extends JpaCrudService<DidacticContent, DidacticContentDto, UUID, DidacticContentRepository> {
    private static final String IMAGES_PATH = "didactic_content";

    private final Storage storage;

    protected DidacticContentService(DidacticContentRepository repository, EntityManager entityManager, Storage storage) {
        super(repository, DidacticContent.class, entityManager);
        this.storage = storage;
    }

    @SneakyThrows
    @Override
    protected void mapModel(DidacticContentDto dto, DidacticContent model) {
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
    protected Page<DidacticContent> search(String search, Pageable pageable, Filter filter) {
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
