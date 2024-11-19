package ec.gob.conagopare.sona.modules.content.services;

import ec.gob.conagopare.sona.application.common.utils.FileUtils;
import ec.gob.conagopare.sona.application.common.utils.StorageUtils;
import ec.gob.conagopare.sona.modules.content.dto.TipDto;
import ec.gob.conagopare.sona.modules.content.models.Tip;
import ec.gob.conagopare.sona.modules.content.repositories.TipRepository;
import io.github.luidmidev.springframework.data.crud.jpa.JpaCRUDService;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import io.github.luidmidev.storage.Storage;
import io.github.luidmidev.storage.Stored;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class TipService extends JpaCRUDService<Tip, TipDto, UUID, TipRepository> {

    private static final String TIPS_IMAGES_PATH = "tips";

    private final Storage storage;

    public TipService(TipRepository repository, EntityManager entityManager, Storage storage) {
        super(repository, Tip.class, entityManager);
        this.storage = storage;
    }

    @Override
    protected void mapModel(TipDto dto, Tip model, MapAction action) {
        var image = dto.getImage();
        if (image != null) {
            try {
                if (action == MapAction.CREATE) {
                    setImage(model, image);
                } else if (action == MapAction.UPDATE) {
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

    public List<Tip> active() {
        return repository.findAllByActiveTrue();
    }

    public Stored image(UUID id) throws IOException {
        var imagePath = repository.getImagePathById(id).orElseThrow(() -> ApiError.notFound("Tip no encontrado: " + id));
        return storage.download(imagePath).orElseThrow(() -> ApiError.notFound("No se encontró la imagen"));
    }

    public void deleteImage(UUID id) {
        var imagePath = repository.findById(id).orElseThrow(() -> ApiError.notFound("Tip no encontrado: " + id));

        imagePath.setImage(null);
        repository.save(imagePath);
        StorageUtils.tryRemoveFileAsync(storage, imagePath.getImage());
    }


    private void setImage(Tip model, MultipartFile image) throws IOException {
        var fileName = "tip-img-" + LocalDateTime.now().toString().replace(":", "-") + "." + FileUtils.getExtension(Objects.requireNonNull(image.getOriginalFilename()));
        var path = storage.store(image.getInputStream(), fileName, TIPS_IMAGES_PATH);
        model.setImage(path);

    }


}
