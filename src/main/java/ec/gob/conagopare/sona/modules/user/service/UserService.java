package ec.gob.conagopare.sona.modules.user.service;

import ec.gob.conagopare.sona.application.common.utils.CollectionsUtils;
import ec.gob.conagopare.sona.application.common.utils.functions.FunctionThrowable;
import ec.gob.conagopare.sona.application.common.utils.FileUtils;
import ec.gob.conagopare.sona.application.common.utils.StorageUtils;
import ec.gob.conagopare.sona.application.common.validations.SonaPassword;
import ec.gob.conagopare.sona.application.configuration.keycloak.KeycloakUserManager;
import ec.gob.conagopare.sona.modules.user.UserConfig;
import ec.gob.conagopare.sona.modules.user.dto.UserDto;
import ec.gob.conagopare.sona.modules.user.dto.SingUpUser;
import ec.gob.conagopare.sona.application.configuration.keycloak.KeycloakUserSync;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import ec.gob.conagopare.sona.modules.user.models.User;
import ec.gob.conagopare.sona.modules.user.repositories.UserRepository;
import io.github.luidmidev.jakarta.validations.Image;
import io.github.luidmidev.springframework.data.crud.core.services.hooks.CrudHooks;
import io.github.luidmidev.springframework.data.crud.jpa.services.JpaCrudService;
import io.github.luidmidev.springframework.data.crud.jpa.utils.AdditionsSearch;
import io.github.luidmidev.springframework.web.problemdetails.ProblemDetails;
import io.github.luidmidev.springframework.web.problemdetails.ProblemDetailsException;
import io.github.luidmidev.storage.Storage;
import io.github.luidmidev.storage.Stored;
import jakarta.persistence.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Service
public class UserService implements JpaCrudService<User, UserDto, Long, UserRepository> {

    private static final String USERS_PROFILE_PICTURES_PATH = "users/%d/profile-pictures";

    private final UserRepository repository;
    private final EntityManager entityManager;
    private final UserConfig config;
    private final KeycloakUserManager keycloakUserManager;
    private final Storage storage;
    private final String clientId;
    private final Environment environment;

    public UserService(UserRepository repository, EntityManager entityManager, UserConfig config, KeycloakUserManager keycloakUserManager, Storage storage, @Value("${keycloak.client-id}") String clientId, Environment environment) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.config = config;
        this.keycloakUserManager = keycloakUserManager;
        this.storage = storage;
        this.clientId = clientId;
        this.environment = environment;
    }

    @PreAuthorize("isAuthenticated()")
    public User getCurrentUser() {
        var jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return getUser(jwt);
    }

    @Override
    public Class<User> getEntityClass() {
        return User.class;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void readyEvent() {
        var bootstrap = config.getBootstrap();
        if (!bootstrap.isEnabled()) return;
        var admin = bootstrap.getAdmin();
        if (admin != null && !repository.existsById(admin.getId())) {
            var singUpUser = admin.toSingUpUser();
            try {
                internalCreate(singUpUser.toRepresentation(), singUpUser.getPassword(), Authority.ADMIN);
            } catch (ProblemDetailsException e) {
                log.info("No se pudo crear el usuario administrador", e);
            }
        }
    }

    @PreAuthorize("permitAll()")
    public void signUp(@Valid SingUpUser singUpUser) {
        internalCreate(singUpUser.toRepresentation(), singUpUser.getPassword(), Authority.USER);
    }

    @Override
    public void mapModel(UserDto dto, User model) {

        var representation = dto.toRepresentation();
        var username = representation.getUsername();
        var email = representation.getEmail();


        if (model.isNew()) {
            validateEmailAndUsername(email, username);
            var authorityToAdd = dto.getAuthoritiesToAdd();
            var password = dto.getPassword();

            if (authorityToAdd.isEmpty()) {
                throw ProblemDetails.badRequest("No se puede crear un usuario sin roles");
            }
            if (password == null) {
                throw ProblemDetails.badRequest("No se puede crear un usuario sin contraseña");
            }

            validateAuthorities(authorityToAdd);

            var keycloakId = keycloakUserManager.create(representation);
            model.setKeycloakId(keycloakId);
        }
    }

    @PreAuthorize("hasAnyRole('admin', 'administrative')")
    public void enable(long userId, boolean enabled, Jwt jwt) {
        var user = getUser(jwt);
        var userToEnable = getUser(userId);
        if (userToEnable.is(Authority.ADMIN)) {
            throw ProblemDetails.badRequest("Esta acción no está permitida en usuarios administradores");
        }

        if (userToEnable.is(Authority.ADMINISTRATIVE) && !user.is(Authority.ADMIN)) {
            throw ProblemDetails.badRequest("No tiene permisos para habilitar/deshabilitar usuarios administrativos");
        }

        keycloakUserManager.enabled(userToEnable.getKeycloakId(), enabled);
    }

    private static void validateAuthorities(Set<Authority> authorities) {
        if (authorities.isEmpty()) return;
        if (authorities.contains(Authority.LEGAL_PROFESSIONAL) && authorities.contains(Authority.MEDICAL_PROFESSIONAL)) {
            throw ProblemDetails.badRequest("No se puede asignar roles de profesional legal y profesional médico al mismo usuario");
        }
    }

    @PreAuthorize("isAuthenticated()")
    public void anonymize(Jwt jwt, boolean anonymize) {
        var user = getUser(jwt);
        user.setAnonymous(anonymize);
        repository.save(user);
    }


    @PreAuthorize("isAuthenticated()")
    public Stored profilePicture(Jwt jwt) {
        var user = getUser(jwt);
        return profilePicture(user);
    }

    public Stored profilePicture(long id) {
        var user = repository.findById(id).orElseThrow(() -> ProblemDetails.badRequest("No se encontról la foto de perfil"));
        return profilePicture(user);
    }

    @PreAuthorize("isAuthenticated()")
    public void uploadProfilePicture(@Image MultipartFile photo, Jwt jwt) throws IOException {
        var user = getUser(jwt);

        var previousProfilePicturePath = user.getProfilePicturePath();
        var profilePictureName = FileUtils.factoryUUIDFileName(photo.getOriginalFilename());
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
            throw ProblemDetails.notFound("Profile picture not found");
        }

        user.setProfilePicturePath(null);
        repository.save(user);

        StorageUtils.tryRemoveFileAsync(storage, previousProfilePicturePath);
    }

    @PreAuthorize("isAuthenticated()")
    public User getUser(Jwt jwt) {
        return getUser(jwt.getSubject());
    }

    @PreAuthorize("isAuthenticated()")
    public User getUser(Long userId) {
        return repository.findById(userId)
                .orElseThrow(() -> ProblemDetails.notFound("Usuario no encontrado"));
    }

    private User getUser(String keycloakId) {
        return repository.findByKeycloakId(keycloakId).orElseThrow(() -> ProblemDetails.notFound("Usuario no encontrado"));
    }

    @PreAuthorize("isAuthenticated()")
    public User profile(Jwt jwt) {
        return getUser(jwt);
    }

    @Override
    public Page<User> internalSearch(String search, Pageable pageable, MultiValueMap<String, String> params) {
        var additions = new AdditionsSearch<User>();

        additions.and((root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();

            var authorities = params.get("authorities");
            if (authorities != null && !authorities.isEmpty()) {
                predicates.add(root.join("authorities").in(Authority.valuesOf(authorities)));
            }

            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        });

        return internalSearch(search, pageable, additions);
    }

    private Stored profilePicture(User user) {
        log.info("Obteniendo foto de perfil del usuario: {} en {}", user.getId(), user.getProfilePicturePath());
        return Optional.ofNullable(user.getProfilePicturePath())
                .map(FunctionThrowable.unchecked(storage::download))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .orElseThrow(() -> ProblemDetails.notFound("No se encontró la foto de perfil"));
    }

    private void internalCreate(UserRepresentation representation, String password, Authority... authority) {

        var email = representation.getEmail();
        var username = representation.getUsername();

        validateEmailAndUsername(email, username);

        var keycloakId = keycloakUserManager.create(representation);

        try {
            repository.save(User.builder()
                    .keycloakId(keycloakId)
                    .build());

            keycloakUserManager.resetPassword(keycloakId, password);
            keycloakUserManager.addRoles(keycloakId, Authority.convertToRoleNames(authority));
        } catch (Exception e) {
            keycloakUserManager.delete(keycloakId);
            throw e;
        }
    }

    private void validateEmailAndUsername(String email, String username) {
        if (keycloakUserManager.searchByEmail(email).isPresent()) {
            throw ProblemDetails.conflict("Conflicto", "El usuario con el correo electrónico " + email + " ya existe");
        }

        if (keycloakUserManager.searchByUsername(username).isPresent()) {
            throw ProblemDetails.conflict("Conflicto", "El usuario con el nombre de usuario " + username + " ya existe");
        }
    }

    public void changePassword(Jwt jwt, @SonaPassword String newPassword) {
        keycloakUserManager.resetPassword(jwt.getSubject(), newPassword);
    }

    public void resetPassword(String emailOrUsername) {
        var isEmail = emailOrUsername.contains("@");

        var user = isEmail
                ? keycloakUserManager.searchByEmail(emailOrUsername)
                : keycloakUserManager.searchByUsername(emailOrUsername);

        user.ifPresent(u -> keycloakUserManager.sendEmailResetPassword(u.getId()));
    }

    public Map<Long, User> map(Iterable<Long> userIds) {
        return repository.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }

    public void syncKeycloak(KeycloakUserSync userSync, String apiKey) {
        log.info("Sincronizando usuarios con Keycloak: {}", userSync);

        var key = config.getSyncApiKey();
        if (!key.equals(apiKey)) {
            throw ProblemDetails.forbidden("API Key inválida");
        }

        var user = getUser(userSync.userId());

        user.setUsername(userSync.username());
        user.setFirstName(userSync.firstName());
        user.setLastName(userSync.lastName());
        user.setEnabled(userSync.enabled());
        user.setEmail(userSync.email());

        var clientRoles = userSync.clientRoles().get(clientId);
        if (clientRoles != null) {
            var currentAuthorities = user.getAuthorities();
            var newAuthorities = Authority.parseAuthorities(clientRoles);
            CollectionsUtils.merge(currentAuthorities, newAuthorities);
        }

        repository.save(user);
        log.info("Sincronizando usuarios con Keycloak: {}", userSync);
    }

    private final CrudHooks<User, UserDto, Long> hooks = new CrudHooks<>() {
        @Override
        public void onAfterCreate(UserDto dto, User model) {

            if (environment.acceptsProfiles(Profiles.of("mockTest"))) {
                return;
            }

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    var keycloakId = model.getKeycloakId();
                    keycloakUserManager.addRoles(keycloakId, Authority.convertToRoleNames(dto.getAuthoritiesToAdd()));
                    keycloakUserManager.resetPassword(keycloakId, dto.getPassword());
                }

                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED && model.isNew() && model.getKeycloakId() != null) {
                        keycloakUserManager.delete(model.getKeycloakId());
                    }
                }
            });
        }

        @Override
        public void onAfterUpdate(UserDto dto, User model) {

            if (environment.acceptsProfiles(Profiles.of("mockTest"))) {
                return;
            }

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    updateKeycloackUser(model.getKeycloakId(), dto);
                }
            });
        }

        private void updateKeycloackUser(String keycloakId, UserDto userDto) {
            var authoritiesToRemove = userDto.getAuthoritiesToRemove();
            var authoritiesToAdd = userDto.getAuthoritiesToAdd();

            validateAuthorities(authoritiesToAdd);

            keycloakUserManager.update(keycloakId, userDto::transferToRepresentation);

            var password = userDto.getPassword();
            if (password != null) {
                keycloakUserManager.resetPassword(keycloakId, password);
            }

            keycloakUserManager.removeRoles(keycloakId, Authority.convertToRoleNames(authoritiesToRemove));
            keycloakUserManager.addRoles(keycloakId, Authority.convertToRoleNames(authoritiesToAdd));
        }
    };


}