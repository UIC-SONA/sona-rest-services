package ec.gob.conagopare.sona.modules.user.service;

import ec.gob.conagopare.sona.application.common.utils.UserRepresentationUtils;
import ec.gob.conagopare.sona.application.common.utils.functions.Extractor;
import ec.gob.conagopare.sona.application.common.utils.functions.FunctionThrowable;
import ec.gob.conagopare.sona.application.common.utils.FileUtils;
import ec.gob.conagopare.sona.application.common.utils.StorageUtils;
import ec.gob.conagopare.sona.application.configuration.keycloak.KeycloakUserManager;
import ec.gob.conagopare.sona.modules.user.UserConfig;
import ec.gob.conagopare.sona.modules.user.dto.BaseUser;
import ec.gob.conagopare.sona.modules.user.dto.UserDto;
import ec.gob.conagopare.sona.modules.user.dto.SingUpUser;
import ec.gob.conagopare.sona.modules.user.dto.KeycloakUserSync;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import ec.gob.conagopare.sona.modules.user.models.User;
import ec.gob.conagopare.sona.modules.user.repositories.UserRepository;
import io.github.luidmidev.jakarta.validations.Image;
import io.github.luidmidev.springframework.data.crud.core.filters.Filter;
import io.github.luidmidev.springframework.data.crud.core.filters.FilterOperator;
import io.github.luidmidev.springframework.data.crud.core.filters.FilterProcessor;
import io.github.luidmidev.springframework.data.crud.core.filters.FilterProcessor.FilterMatcher;
import io.github.luidmidev.springframework.data.crud.jpa.services.JpaCrudService;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import io.github.luidmidev.storage.Storage;
import io.github.luidmidev.storage.Stored;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
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
            var user = new User();
            var keycloackId = createKeycloakUser(singUpUser.toRepresentation(), singUpUser.getPassword(), Authority.ADMIN);
            user.setKeycloakId(keycloackId);
            repository.save(user);
        }
    }

    @PreAuthorize("permitAll()")
    public void signUp(@Valid SingUpUser singUpUser) {
        var user = new User();
        var keycloackId = createKeycloakUser(singUpUser.toRepresentation(), singUpUser.getPassword(), Authority.USER);
        user.setKeycloakId(keycloackId);
        repository.save(user);
    }


    @Override
    protected void mapModel(UserDto dto, User model) {
        if (model.isNew()) {
            var authorityToAdd = dto.getAuthoritiesToAdd();
            var password = dto.getPassword();

            if (authorityToAdd.isEmpty()) {
                throw ApiError.badRequest("No se puede crear un usuario sin roles");
            }

            if (password == null) {
                throw ApiError.badRequest("No se puede crear un usuario sin contraseña");
            }

            validateAuthorities(authorityToAdd);
            var keycloackId = createKeycloakUser(dto.toRepresentation(), password, authorityToAdd.toArray(Authority[]::new));
            model.setKeycloakId(keycloackId);
        } else {
            updateKeycloackUser(model.getKeycloakId(), dto);
        }

        dispatchUser(model);
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

    @PreAuthorize("isAuthenticated()")
    public Stored profilePicture(Long id) {
        var user = getUser(id);
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
            throw ApiError.notFound("Profile picture not found");
        }

        user.setProfilePicturePath(null);
        repository.save(user);

        StorageUtils.tryRemoveFileAsync(storage, previousProfilePicturePath);
    }

    @PreAuthorize("isAuthenticated()")
    public User getUser(Jwt jwt) {
        return dispatchUser(repository.findByKeycloakId(jwt.getSubject()).orElseThrow(() -> ApiError.notFound("Usuario no encontrado")));
    }

    @PreAuthorize("isAuthenticated()")
    public User getUser(Long userId) {
        return dispatchUser(repository.findById(userId).orElseThrow(() -> ApiError.notFound("Usuario no encontrado")));
    }

    @PreAuthorize("isAuthenticated()")
    public User profile(Jwt jwt) {
        return getUser(jwt);
    }

    @Override
    public Page<User> search(String search, Pageable pageable) {
        var representations = keycloakUserManager.search(search, pageable);
        return representations.map(mapToUser(representations.toList()));
    }

    @Override
    protected Page<User> search(String search, Pageable pageable, Filter filter) {
        return FilterProcessor.process(filter,
                () -> {
                    throw ApiError.badRequest("Filtro no soportado");
                },
                FilterProcessor
                        .of(new FilterMatcher("role", FilterOperator.EQ))
                        .resolve(values -> {
                            var role = Authority.valueOf(values[0].toString());
                            return searchFilterRole(search, role, pageable);
                        })
        );
    }

    private Stored profilePicture(User user) {
        return Optional.ofNullable(user.getProfilePicturePath())
                .map(FunctionThrowable.unchecked(storage::download))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .orElseThrow(() -> ApiError.notFound("No se encontró la foto de perfil"));
    }

    private Page<User> searchFilterRole(String search, Authority role, Pageable pageable) {
        var representations = findUserRepresentations(search, role);
        if (pageable.isUnpaged()) {
            return PageableExecutionUtils.getPage(
                    representations.stream().map(mapToUser(representations)).toList(),
                    pageable,
                    representations::size
            );
        }

        var result = UserRepresentationUtils
                .sort(representations, pageable.getSort())
                .stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .map(mapToUser(representations))
                .toList();

        return PageableExecutionUtils.getPage(
                result,
                pageable,
                representations::size
        );
    }

    @Override
    protected void onFind(User model) {
        dispatchUser(model);
    }

    @Override
    protected void onList(Iterable<User> models) {
        models.forEach(this::dispatchUser);
    }

    @Override
    protected void onPage(Page<User> page) {
        page.forEach(this::dispatchUser);
    }

    public User dispatchUser(User user) {
        var representation = keycloakUserManager.get(user.getKeycloakId());
        return dispatchUser(user, representation);
    }

    private User dispatchUser(User user, UserRepresentation representation) {
        var authorities = authorityExtractor.extract(representation);

        user.setRepresentation(representation);
        user.setAuthorities(authorities);
        return user;
    }

    public Consumer<User> setRepresentations() {
        var representations = new ArrayList<UserRepresentation>();
        return u -> {
            var keycloakId = u.getKeycloakId();
            var representation = representations.stream()
                    .filter(r -> r.getId().equals(keycloakId))
                    .findFirst()
                    .orElseGet(() -> {
                        var r = keycloakUserManager.get(keycloakId);
                        representations.add(r);
                        return r;
                    });

            dispatchUser(u, representation);
        };
    }

    private String createKeycloakUser(UserRepresentation representation, String password, Authority... authority) {
        return keycloakUserManager.create(representation, password, Authority.getAuthorities(authority));
    }

    private void updateKeycloackUser(String keycloakId, BaseUser userDto) {
        keycloakUserManager.update(keycloakId, userDto::transferToRepresentation);
    }

    private void updateKeycloackUser(String keycloakId, UserDto userDto) {
        updateKeycloackUser(keycloakId, (BaseUser) userDto);

        var password = userDto.getPassword();
        if (password != null) {
            keycloakUserManager.resetPassword(keycloakId, password);
        }

        var authoritiesToRemove = userDto.getAuthoritiesToRemove();
        var authoritiesToAdd = userDto.getAuthoritiesToAdd();

        validateAuthorities(authoritiesToAdd);

        keycloakUserManager.removeRoles(keycloakId, Authority.getAuthorities(authoritiesToRemove));
        keycloakUserManager.addRoles(keycloakId, Authority.getAuthorities(authoritiesToAdd));
    }

    private List<User> findUsers(List<UserRepresentation> representations) {
        var keycloakIds = extractKeycloakIds(representations);
        return repository.findAllByKeycloakIdIn(keycloakIds);
    }

    private Function<UserRepresentation, User> mapToUser(List<UserRepresentation> representations) {
        var users = findUsers(representations);
        return representation -> {
            var user = users.stream()
                    .filter(u -> u.getKeycloakId().equals(representation.getId()))
                    .findFirst()
                    .orElseThrow(() -> ApiError.internalServerError("Inconsistencia de datos, usuario no encontrado. Keycloak id: " + representation.getId()));

            return dispatchUser(user, representation);
        };
    }

    private List<UserRepresentation> findUserRepresentations(String search, Authority role) {
        var representations = findUserRepresentations(role);
        return search == null ? representations : representations
                .stream()
                .filter(UserRepresentationUtils.filterPredicate(search))
                .toList();
    }

    private List<UserRepresentation> findUserRepresentations(Authority role) {
        return keycloakUserManager.searchByRole(role.getAuthority());
    }

    private static void validateAuthorities(Set<Authority> authorities) {
        boolean hasLegalProfessional = authorities.contains(Authority.LEGAL_PROFESSIONAL);
        boolean hasMedicalProfessional = authorities.contains(Authority.MEDICAL_PROFESSIONAL);

        if (hasLegalProfessional && hasMedicalProfessional) {
            throw ApiError.badRequest("No se puede asignar roles de profesional legal y profesional médico al mismo usuario");
        }

        if (hasLegalProfessional || hasMedicalProfessional) {
            authorities.add(Authority.PROFESSIONAL);
        }
    }

    private static List<String> extractKeycloakIds(List<UserRepresentation> representations) {
        return representations.stream().map(UserRepresentation::getId).toList();
    }

    @PreAuthorize("isAuthenticated()")
    public void changePassword(Jwt jwt, String newPassword) {
        keycloakUserManager.resetPassword(jwt.getSubject(), newPassword);
    }

    public void syncKeycloak(KeycloakUserSync userSync, String apiKey) {

        var key = config.getSyncApiKey();

        if (!key.equals(apiKey)) {
            throw ApiError.forbidden("API Key inválida");
        }

        log.info("Sincronizando usuarios con Keycloak: {}", userSync);
    }
}