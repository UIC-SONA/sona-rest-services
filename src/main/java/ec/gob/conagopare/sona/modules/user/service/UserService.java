package ec.gob.conagopare.sona.modules.user.service;

import ec.gob.conagopare.sona.application.common.utils.functions.Extractor;
import ec.gob.conagopare.sona.application.common.utils.functions.FunctionThrowable;
import ec.gob.conagopare.sona.application.common.utils.FileUtils;
import ec.gob.conagopare.sona.application.common.utils.StorageUtils;
import ec.gob.conagopare.sona.application.configuration.keycloak.KeycloakUserManager;
import ec.gob.conagopare.sona.modules.user.UserConfig;
import ec.gob.conagopare.sona.modules.user.dto.SingUpUser;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import ec.gob.conagopare.sona.modules.user.models.User;
import ec.gob.conagopare.sona.modules.user.repositories.UserRepository;
import io.github.luidmidev.jakarta.validations.Image;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import io.github.luidmidev.storage.Storage;
import io.github.luidmidev.storage.Stored;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.keycloak.representations.idm.UserRepresentation;
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
@RequiredArgsConstructor
public class UserService {

    private static final String USERS_PROFILE_PICTURES_PATH = "users/%d/profile-pictures";

    private final UserConfig config;
    private final UserRepository repository;
    private final KeycloakUserManager keycloakUserManager;
    private final Storage storage;
    private final Extractor<UserRepresentation, Collection<Authority>> authorityExtractor;

    @PostConstruct
    public void init() {
        var bootstrap = config.getBootstrap();

        if (!bootstrap.isEnabled()) return;

        var admin = bootstrap.getAdmin();

        if (admin != null && !repository.existsById(admin.getId())) {
            createUser(admin.toInfo(), user -> user.setId(admin.getId()), Authority.ADMIN);
        }
    }

    public void signUp(@Valid SingUpUser singUpUser) {
        createUser(singUpUser, Authority.USER);
    }

    public Stored profilePicture(Jwt jwt) {
        var user = getUser(jwt);
        return Optional.ofNullable(user.getProfilePicturePath())
                .map(FunctionThrowable.unchecked(storage::download))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .orElseThrow(() -> ApiError.notFound("No se encontró la foto de perfil"));
    }

    public void uploadProfilePicture(@Image MultipartFile photo, Jwt jwt) throws IOException {
        var user = getUser(jwt);

        var previousProfilePicturePath = user.getProfilePicturePath();
        var profilePictureName = UUID.randomUUID() + "." + FileUtils.getExtension(Objects.requireNonNull(photo.getOriginalFilename()));
        var profilePicturePath = storage.store(photo.getInputStream(), profilePictureName, USERS_PROFILE_PICTURES_PATH.formatted(user.getId()));
        user.setProfilePicturePath(profilePicturePath);

        repository.save(user);

        StorageUtils.tryRemoveFileAsync(storage, previousProfilePicturePath);
    }

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

    public List<User> users() {
        return repository.findAll();
    }

    private void createUser(SingUpUser newUser, Authority authority) {
        createUser(newUser, user -> {
        }, authority);
    }

    private void createUser(SingUpUser newUser, Consumer<User> beforeSave, Authority authority) {

        var keycloakId = keycloakUserManager.create(
                newUser.toUserRepresentation(),
                newUser.getPassword(),
                authority.getAuthority()
        );

        var user = User.builder()
                .keycloakId(keycloakId)
                .build();

        beforeSave.accept(user);
        repository.save(user);
    }
}
