package ec.gob.conagopare.sona.modules.content.services;

import ec.gob.conagopare.sona.application.common.utils.FileUtils;
import ec.gob.conagopare.sona.application.common.utils.StorageUtils;
import ec.gob.conagopare.sona.modules.content.dto.TipDto;
import ec.gob.conagopare.sona.modules.content.models.Tip;
import ec.gob.conagopare.sona.modules.content.models.TipRate;
import ec.gob.conagopare.sona.modules.content.repositories.TipRepository;
import ec.gob.conagopare.sona.modules.content.repositories.TipValuationRepository;
import ec.gob.conagopare.sona.modules.user.models.User;
import ec.gob.conagopare.sona.modules.user.service.NotificationService;
import ec.gob.conagopare.sona.modules.user.service.UserService;
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
import org.hibernate.validator.constraints.Range;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
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
        var actives = search == null ? repository.findAllByActiveTrue(pageable) : internalSearch(search, pageable, AND_ACTIVE_TRUE);
        hooks.onPage(actives);
        return actives;
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

    @PreAuthorize("isAuthenticated()")
    public void rate(UUID id, @Range(min = 1, max = 5) int value) {
        var currentUser = userService.getCurrentUser();
        var tip = find(id);

        var valuationEntity = valuationRepository.findByTipIdAndUserId(id, currentUser.getId())
                .orElseGet(() -> valuationRepository.save(TipRate.builder()
                        .tip(tip)
                        .user(currentUser)
                        .valuationDate(LocalDateTime.now())
                        .build())
                );

        valuationEntity.setValue(value);
        valuationRepository.save(valuationEntity);
    }

    private void setImage(Tip model, MultipartFile image) throws IOException {
        var fileName = FileUtils.factoryDateTimeFileName("tip-img-", image.getOriginalFilename());
        var path = storage.store(image.getInputStream(), fileName, TIPS_IMAGES_PATH);
        model.setImage(path);
    }

    @Override
    public Page<Tip> internalSearch(String search, Pageable pageable, MultiValueMap<String, String> params) {
        return internalSearch(search, pageable);
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

        @Override
        public void onFind(Tip entity) {
            var currentUser = userService.getCurrentUser();
            addRateInfo(entity, currentUser);
        }

        @Override
        public void onFind(Iterable<Tip> entities, Iterable<UUID> ids) {
            var currentUser = userService.getCurrentUser();
            entities.forEach(entity -> addRateInfo(entity, currentUser));
        }

        @Override
        public void onPage(Page<Tip> page) {
            var currentUser = userService.getCurrentUser();
            page.forEach(entity -> addRateInfo(entity, currentUser));
        }

        private void addRateInfo(Tip entity, User currentUser) {
            var myValuation = valuationRepository.fingUserValuation(entity.getId(), currentUser.getId());
            var valuations = valuationRepository.findValuations(entity.getId());
            var valuationsCount = valuations.size();
            entity.setMyRate(myValuation);
            entity.setAverageRate(valuations.stream().mapToInt(Integer::intValue).average().orElse(0));
            entity.setTotalRate(valuationsCount);
        }
    };

    @PreAuthorize("isAuthenticated()")
    public List<Tip> top() {
        var topTips = repository.topRating(3);
        hooks.onFind(topTips, null);
        return topTips;
    }
}
