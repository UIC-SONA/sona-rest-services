package ec.gob.conagopare.sona.modules.content.services;

import ec.gob.conagopare.sona.application.common.utils.StorageUtils;
import ec.gob.conagopare.sona.modules.content.dto.TipDto;
import ec.gob.conagopare.sona.modules.content.models.Tip;
import ec.gob.conagopare.sona.modules.content.repositories.TipRepository;
import io.github.luidmidev.springframework.data.crud.jpa.JpaCRUDService;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import io.github.luidmidev.storage.Storage;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class TipService extends JpaCRUDService<Tip, TipDto, UUID, TipRepository> {

    private final Storage storage;

    public TipService(TipRepository repository, EntityManager entityManager, Storage storage) {
        super(repository, Tip.class, entityManager);
        this.storage = storage;
    }

    @Override
    protected void mapModel(TipDto dto, Tip model, MapAction action) {

        try {

            if (action == MapAction.CREATE) {
                setImage(dto, model);
            } else if (action == MapAction.UPDATE) {
                var oldImage = model.getImage();
                setImage(dto, model);
                StorageUtils.tryRemoveFileAsync(storage, oldImage);
            }

        } catch (IOException e) {
            throw ApiError.internalServerError("Error al guardar la imagen: " + e.getMessage());
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

    private void setImage(TipDto dto, Tip model) throws IOException {
        var image = dto.getImage();
        if (image != null) {
            var path = storage.store(image.getInputStream(), image.getOriginalFilename());
            model.setImage(path);
        }
    }
}
