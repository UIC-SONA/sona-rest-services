package ec.gob.conagopare.sona.modules.user.service;

import ec.gob.conagopare.sona.application.common.utils.functions.Extractor;
import ec.gob.conagopare.sona.application.common.utils.functions.FunctionThrowable;
import ec.gob.conagopare.sona.application.common.utils.FileUtils;
import ec.gob.conagopare.sona.application.common.utils.StorageUtils;
import ec.gob.conagopare.sona.application.common.utils.functions.NoOpt;
import ec.gob.conagopare.sona.application.configuration.keycloak.KeycloakUserManager;
import ec.gob.conagopare.sona.modules.user.UserConfig;
import ec.gob.conagopare.sona.modules.user.dto.BaseUser;
import ec.gob.conagopare.sona.modules.user.dto.UserDto;
import ec.gob.conagopare.sona.modules.user.dto.SingUpUser;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import ec.gob.conagopare.sona.modules.user.models.User;
import ec.gob.conagopare.sona.modules.user.repositories.UserRepository;
import io.github.luidmidev.jakarta.validations.Image;
import io.github.luidmidev.springframework.data.crud.jpa.services.JpaCrudService;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import io.github.luidmidev.storage.Storage;
import io.github.luidmidev.storage.Stored;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

@Service
@Validated
@Transactional
public class UserService extends JpaCrudService<User, UserDto, Long, UserRepository> {

    private static final String USERS_PROFILE_PICTURES_PATH = "users/%d/profile-pictures";

    private final UserConfig config;
    private final KeycloakUserManager keycloakUserManager;
    private final Storage storage;
    private final Extractor<UserRepresentation, Collection<Authority>> authorityExtractor;

    public UserService(UserConfig config, UserRepository repository, EntityManager entityManager, KeycloakUserManager keycloakUserManager, Storage storage, Extractor<UserRepresentation, Collection<Authority>> authorityExtractor) {
        super(repository, User.class, entityManager);
        this.config = config;
        this.keycloakUserManager = keycloakUserManager;
        this.storage = storage;
        this.authorityExtractor = authorityExtractor;
    }

    @PostConstruct
    public void init() {
        var bootstrap = config.getBootstrap();

        if (!bootstrap.isEnabled()) return;

        var admin = bootstrap.getAdmin();

        if (admin != null && !repository.existsById(admin.getId())) {
            var singUpUser = admin.toSingUpUser();
            create(singUpUser.toRepresentation(), singUpUser.getPassword(), user -> user.setId(admin.getId()), Authority.ADMIN);
        }
    }

    @PreAuthorize("permitAll()")
    public void signUp(@Valid SingUpUser singUpUser) {
        create(singUpUser.toRepresentation(), singUpUser.getPassword(), Authority.USER);
    }


    @Override
    protected void mapModel(UserDto dto, User model) {
        if (model.isNew()) {
            var authorityToAdd = dto.getAuthorityToAdd();
            var password = dto.getPassword();

            if (authorityToAdd.isEmpty()) {
                throw ApiError.badRequest("No se puede crear un usuario sin roles");
            }

            if (password == null) {
                throw ApiError.badRequest("No se puede crear un usuario sin contraseña");
            }

            create(dto.toRepresentation(), password, authorityToAdd.toArray(Authority[]::new));
        } else {
            update(model, dto);
        }

        dispathUser(model);
    }

    @PreAuthorize("isAuthenticated()")
    public Stored profilePicture(Jwt jwt) {
        var user = getUser(jwt);
        return Optional.ofNullable(user.getProfilePicturePath())
                .map(FunctionThrowable.unchecked(storage::download))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .orElseThrow(() -> ApiError.notFound("No se encontró la foto de perfil"));
    }

    @PreAuthorize("isAuthenticated()")
    public void uploadProfilePicture(@Image MultipartFile photo, Jwt jwt) throws IOException {
        var user = getUser(jwt);

        var previousProfilePicturePath = user.getProfilePicturePath();
        var profilePictureName = UUID.randomUUID() + "." + FileUtils.getExtension(Objects.requireNonNull(photo.getOriginalFilename()));
        var profilePicturePath = storage.store(photo.getInputStream(), profilePictureName, USERS_PROFILE_PICTURES_PATH.formatted(user.getId()));
        user.setProfilePicturePath(profilePicturePath);

        repository.save(user);

        StorageUtils.tryRemoveFileAsync(storage, previousProfilePicturePath);
    }

    @PreAuthorize("isAuthenticated()")
    public void deleteProfilePicture(Jwt jwt) {
        var user = getUser(jwt);

        var previousProfilePicturePath = user.getProfilePicturePath();
        if (previousProfilePicturePath == null) {
            throw ApiError.notFound("Profile picture not found");
        }

        user.setProfilePicturePath(null);
        repository.save(user);

        StorageUtils.tryRemoveFileAsync(storage, previousProfilePicturePath);
    }


    public User getUser(Jwt jwt) {
        return dispathUser(repository.findByKeycloakId(jwt.getSubject()).orElseThrow(() -> ApiError.notFound("Usuario no encontrado")));
    }

    public User getUser(Long userId) {
        return dispathUser(repository.findById(userId).orElseThrow(() -> ApiError.notFound("Usuario no encontrado")));
    }


    public User dispathUser(User user) {
        var representation = keycloakUserManager.get(user.getKeycloakId());
        var authorities = authorityExtractor.extract(representation);

        user.setRepresentation(representation);
        user.setAuthorities(authorities);

        return user;
    }

    @Override
    protected void onFind(User model) {
        dispathUser(model);
    }

    @Override
    protected void onList(List<User> models) {
        models.forEach(this::dispathUser);
    }

    @Override
    protected void onPage(Page<User> page) {
        page.forEach(this::dispathUser);
    }

    private void create(UserRepresentation representation, String password, Authority... authority) {
        create(representation, password, NoOpt.consumer(), authority);
    }

    private void create(UserRepresentation representation, String password, Consumer<User> beforeSave, Authority... authority) {

        var keycloakId = keycloakUserManager.create(
                representation,
                password,
                Authority.getAuthorities(authority)
        );

        var user = User.builder()
                .keycloakId(keycloakId)
                .build();

        beforeSave.accept(user);
        repository.save(user);
    }

    private void update(String keycloakId, BaseUser userDto) {
        keycloakUserManager.update(keycloakId, userDto::transferToRepresentation);
    }

    private void update(User user, UserDto userDto) {
        var keycloakId = user.getKeycloakId();
        update(keycloakId, userDto);

        var password = userDto.getPassword();
        if (password != null) {
            keycloakUserManager.resetPassword(keycloakId, password);
        }

        var authoritiesToRemove = userDto.getAuthorityToRemove();
        var authoritiesToAdd = userDto.getAuthorityToAdd();

        keycloakUserManager.removeRoles(keycloakId, Authority.getAuthorities(authoritiesToRemove));
        keycloakUserManager.addRoles(keycloakId, Authority.getAuthorities(authoritiesToAdd));
    }

    @PreAuthorize("authenticated()")
    public User profile(Jwt jwt) {
        return getUser(jwt);
    }
}