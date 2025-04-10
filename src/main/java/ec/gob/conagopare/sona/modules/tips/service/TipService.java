package ec.gob.conagopare.sona.modules.tips.service;

import ec.gob.conagopare.sona.application.common.utils.FileUtils;
import ec.gob.conagopare.sona.application.common.utils.StorageUtils;
import ec.gob.conagopare.sona.modules.tips.dto.TipDto;
import ec.gob.conagopare.sona.modules.tips.models.Tip;
import ec.gob.conagopare.sona.modules.tips.models.TipRate;
import ec.gob.conagopare.sona.modules.tips.repositories.TipRepository;
import ec.gob.conagopare.sona.modules.tips.repositories.TipValuationRepository;
import ec.gob.conagopare.sona.application.firebase.NotificationService;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.springframework.data.crud.core.exceptions.NotFoundEntityException;
import io.github.luidmidev.springframework.data.crud.core.services.hooks.CrudHooks;
import io.github.luidmidev.springframework.data.crud.jpa.services.JpaCrudService;
import io.github.luidmidev.springframework.web.problemdetails.ProblemDetails;
import io.github.luidmidev.storage.Storage;
import io.github.luidmidev.storage.Stored;
import jakarta.persistence.EntityManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Range;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Optional.*;

@Slf4j
@Service
@Getter
@RequiredArgsConstructor
public class TipService implements JpaCrudService<Tip, TipDto, UUID, TipRepository> {

    private static final String TIPS_IMAGES_PATH = "tips";

    private final TipRepository repository;
    private final EntityManager entityManager;
    private final TipValuationRepository valuationRepository;
    private final Storage storage;
    private final NotificationService notificationService;
    private final UserService userService;

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
                throw ProblemDetails.internalServerError("Error al guardar la imagen: " + e.getMessage());
            }
        } else if (model.isNew()) {
            throw ProblemDetails.badRequest("La imagen es requerida");
        }

        model.setTitle(dto.getTitle());
        model.setSummary(dto.getSummary());
        model.setDescription(dto.getDescription());
        model.setTags(dto.getTags() != null ? new ArrayList<>(dto.getTags()) : null);
        model.setActive(dto.isActive());
    }

    @PreAuthorize("isAuthenticated()")
    public Page<Tip> actives(String search, Pageable pageable) {
        var currentUser = userService.getCurrentUser();
        return repository.searchAllWithRates(search, currentUser.getId(), true, pageable);
    }

    @PreAuthorize("isAuthenticated()")
    public Stored image(UUID id) throws IOException {
        var imagePath = repository.getImagePathById(id).orElseThrow(() -> new NotFoundEntityException(getEntityClass(), id));
        return storage.download(imagePath).orElseThrow(() -> ProblemDetails.notFound("No se encontró la imagen"));
    }


    @PreAuthorize("hasRole('admin')")
    public void deleteImage(UUID id) {
        var tip = repository.findById(id).orElseThrow(() -> new NotFoundEntityException(getEntityClass(), id));
        var imagePath = tip.getImage();
        tip.setImage(null);
        repository.save(tip);
        StorageUtils.tryRemoveFileAsync(storage, imagePath);
    }


    @PreAuthorize("isAuthenticated()")
    public void rate(UUID id, @Range(min = 1, max = 5) int value) {
        var currentUser = userService.getCurrentUser();
        var tip = find(id);

        var valuationEntity = valuationRepository.findByTipIdAndUserId(id, currentUser.getId())
                .orElseGet(() -> TipRate.builder()
                        .tip(tip)
                        .user(currentUser)
                        .valuationDate(LocalDateTime.now())
                        .build());

        valuationEntity.setValue(value);
        valuationRepository.save(valuationEntity);
    }

    private void setImage(Tip model, MultipartFile image) throws IOException {
        var fileName = FileUtils.factoryDateTimeFileName("tip-img-", image.getOriginalFilename());
        var path = storage.store(image.getInputStream(), fileName, TIPS_IMAGES_PATH);
        model.setImage(path);
    }

    @Override
    public Tip internalFind(UUID uuid) {
        var currentUser = userService.getCurrentUser();
        return repository.findByIdWithRates(uuid, currentUser.getId()).orElseThrow(() -> new NotFoundEntityException(getEntityClass(), uuid));
    }

    @Override
    public Page<Tip> internalPage(Pageable pageable) {
        var currentUser = userService.getCurrentUser();
        return repository.findAllWithRates(currentUser.getId(), null, pageable);
    }

    @Override
    public Page<Tip> internalSearch(String search, Pageable pageable) {
        return internalSearch(search, pageable, new LinkedMultiValueMap<>());
    }

    @Override
    public Page<Tip> internalSearch(String search, Pageable pageable, MultiValueMap<String, String> params) {
        var currentUser = userService.getCurrentUser();
        var active = ofNullable(params.getFirst("active")).map(Boolean::parseBoolean).orElse(null);
        return repository.searchAllWithRates(search, currentUser.getId(), active, pageable);
    }

    @Override
    public Class<Tip> getEntityClass() {
        return Tip.class;
    }

    private final CrudHooks<Tip, TipDto, UUID> hooks = new CrudHooks<>() {
        @Override
        public void onAfterDelete(Tip model) {
            StorageUtils.tryRemoveFileAsync(storage, model.getImage());
        }

        @Override
        public void onAfterCreate(TipDto dto, Tip model) {
            if (model.isActive()) {
                notificationService.sendAll(
                        "Nuevo consejo para ti",
                        "Hemos agregado un nuevo tip que podría interesarte. Accede a la aplicación para leerlo y aprender más."
                );
            }
        }
    };

    @PreAuthorize("isAuthenticated()")
    public List<Tip> top() {
        return repository.topRating(3);
    }
}
