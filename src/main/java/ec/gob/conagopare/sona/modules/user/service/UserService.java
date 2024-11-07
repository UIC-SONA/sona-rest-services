package ec.gob.conagopare.sona.modules.user.service;

import ec.gob.conagopare.sona.application.common.concurrent.CompletableFutureThrowables;
import ec.gob.conagopare.sona.application.common.functions.FunctionThrowable;
import ec.gob.conagopare.sona.application.common.utils.FileUtils;
import ec.gob.conagopare.sona.application.configuration.keycloak.KeycloakUserManager;
import ec.gob.conagopare.sona.modules.user.dto.SignupUser;
import ec.gob.conagopare.sona.modules.user.dto.UpdateUser;
import ec.gob.conagopare.sona.modules.user.entities.User;
import ec.gob.conagopare.sona.modules.user.repositories.UserRepository;
import io.github.luidmidev.jakarta.validations.Image;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import io.github.luidmidev.storage.Storage;
import io.github.luidmidev.storage.Stored;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class UserService {

    private static final String USERS_PROFILE_PICTURES_PATH = "users/%d/profile-pictures";

    private final UserRepository repository;
    private final KeycloakUserManager keycloakUserManager;
    private final Storage storage;


    public void signup(@Valid SignupUser signupUser) {
        var userRepresentation = signupUser.toUserRepresentation();
        var keycloakId = keycloakUserManager.create(userRepresentation, signupUser.getPassword());

        var user = User.builder()
                .keycloakId(keycloakId)
                .ci(signupUser.getCi())
                .dateOfBirth(signupUser.getDateOfBirth())
                .build();

        repository.save(user);
    }

    public User onboard(@Valid UpdateUser updateUser, Jwt jwt) {
        if (hasOnboarded(jwt)) throw ApiError.conflict("El usuario ya ha sido registrado");

        var user = User.builder()
                .keycloakId(jwt.getSubject())
                .ci(updateUser.getCi())
                .dateOfBirth(updateUser.getDateOfBirth())
                .build();

        return repository.save(user);
    }

    public User update(@Valid UpdateUser updateUser, Jwt jwt) {
        var user = getOnboardedUser(jwt);

        user.setCi(updateUser.getCi());
        user.setDateOfBirth(updateUser.getDateOfBirth());

        var saved = repository.save(user);

        keycloakUserManager.update(user.getKeycloakId(), u -> {
            u.setFirstName(updateUser.getFirstName());
            u.setLastName(updateUser.getLastName());
        });

        return saved;
    }

    public Stored getProfilePicture(Jwt jwt) {
        var user = getOnboardedUser(jwt);
        return Optional.ofNullable(user.getProfilePicturePath())
                .map(FunctionThrowable.unchecked(storage::download))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .orElseThrow(() -> ApiError.notFound("Profile picture not found"));
    }

    public void uploadProfilePicture(@Image MultipartFile photo, Jwt jwt) throws IOException {
        var user = getOnboardedUser(jwt);

        var previousProfilePicturePath = user.getProfilePicturePath();
        var profilePictureName = UUID.randomUUID() + "." + FileUtils.getExtension(Objects.requireNonNull(photo.getOriginalFilename()));
        var profilePicturePath = storage.store(photo.getInputStream(), profilePictureName, USERS_PROFILE_PICTURES_PATH.formatted(user.getId()));
        user.setProfilePicturePath(profilePicturePath);

        repository.save(user);

        if (previousProfilePicturePath != null) {
            CompletableFutureThrowables.runAsync(() -> storage.remove(previousProfilePicturePath));
        }
    }

    public void deleteProfilePicture(Jwt jwt) {
        var user = getOnboardedUser(jwt);

        var previousProfilePicturePath = user.getProfilePicturePath();
        if (previousProfilePicturePath == null) {
            throw ApiError.notFound("Profile picture not found");
        }

        user.setProfilePicturePath(null);
        repository.save(user);

        CompletableFutureThrowables.runAsync(() -> storage.remove(previousProfilePicturePath));
    }

    public boolean hasOnboarded(Jwt jwt) {
        return repository.existsByKeycloakId(jwt.getSubject());
    }

    public User getOnboardedUser(Jwt jwt) {
        return repository.findByKeycloakId(jwt.getSubject()).orElseThrow(() -> ApiError.notFound("User not onboarded"));
    }

    public User getUser(Long userId) {
        return repository.findById(userId).orElseThrow(() -> ApiError.notFound("User not found"));
    }


}
