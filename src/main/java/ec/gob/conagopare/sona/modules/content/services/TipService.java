package ec.gob.conagopare.sona.modules.content.services;

import ec.gob.conagopare.sona.application.common.utils.FileUtils;
import ec.gob.conagopare.sona.application.common.utils.StorageUtils;
import ec.gob.conagopare.sona.modules.content.dto.TipDto;
import ec.gob.conagopare.sona.modules.content.models.Tip;
import ec.gob.conagopare.sona.modules.content.repositories.TipRepository;
import io.github.luidmidev.springframework.data.crud.core.services.hooks.CrudHooks;
import io.github.luidmidev.springframework.data.crud.jpa.services.JpaCrudService;
import io.github.luidmidev.springframework.data.crud.jpa.utils.AdditionsSearch;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import io.github.luidmidev.storage.Storage;
import io.github.luidmidev.storage.Stored;
import jakarta.persistence.EntityManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@Getter
@RequiredArgsConstructor
public class TipService implements JpaCrudService<Tip, TipDto, UUID, TipRepository> {

    private static final String TIPS_IMAGES_PATH = "tips";
    private static final AdditionsSearch<Tip> AND_ACTIVE_TRUE = new AdditionsSearch<Tip>().and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("active"), true));

    private final TipRepository repository;
    private final EntityManager entityManager;
    private final Storage storage;

    @Override
    public Class<Tip> getEntityClass() {
        return Tip.class;
    }

    @Override
    public void mapModel(TipDto dto, Tip model) {
        var image = dto.getImage();
        if (image != null) {
            try {
                if (model.isNew()) {
                    setImage(model, image);
                } else {
                    var oldImage = model.getImage();
                    setImage(model, image);
                    StorageUtils.tryRemoveFileAsync(storage, oldImage);
                }
            } catch (IOException e) {
                throw ApiError.internalServerError("Error al guardar la imagen: " + e.getMessage());
            }
        }

        model.setTitle(dto.getTitle());
        model.setSummary(dto.getSummary());
        model.setDescription(dto.getDescription());
        model.setTags(dto.getTags());
        model.setActive(dto.isActive());
    }

    @PreAuthorize("isAuthenticated()")
    public Page<Tip> actives(String search, Pageable pageable) {
        return search == null ? repository.findAllByActiveTrue(pageable) : internalSearch(search, pageable, AND_ACTIVE_TRUE);
    }

    @PreAuthorize("isAuthenticated()")
    public Stored image(UUID id) throws IOException {
        var imagePath = repository.getImagePathById(id).orElseThrow(() -> ApiError.notFound("Tip no encontrado: " + id));
        return storage.download(imagePath).orElseThrow(() -> ApiError.notFound("No se encontró la imagen"));
    }


    @PreAuthorize("hasRole('admin')")
    public void deleteImage(UUID id) {
        var imagePath = repository.findById(id).orElseThrow(() -> ApiError.notFound("Tip no encontrado: " + id));

        imagePath.setImage(null);
        repository.save(imagePath);
        StorageUtils.tryRemoveFileAsync(storage, imagePath.getImage());
    }

    private void setImage(Tip model, MultipartFile image) throws IOException {
        var fileName = FileUtils.factoryDateTimeFileName("tip-img-", image.getOriginalFilename());
        var path = storage.store(image.getInputStream(), fileName, TIPS_IMAGES_PATH);
        model.setImage(path);
    }

    @Override
    public Page<Tip> internalSearch(String search, Pageable pageable, MultiValueMap<String, String> params) {
        throw ApiError.badRequest("Filtro no soportado");
    }

    private final CrudHooks<Tip, TipDto, UUID> hooks = new CrudHooks<>() {
        @Override
        public void onAfterCreate(TipDto dto, Tip model) {
            StorageUtils.tryRemoveFileAsync(storage, model.getImage());
        }
    };
}
