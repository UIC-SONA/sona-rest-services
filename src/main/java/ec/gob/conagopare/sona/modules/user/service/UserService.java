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
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
            var authorityToAdd = dto.getAuthoritiesToAdd();
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


    public User getUser(Jwt jwt) {
        return dispathUser(repository.findByKeycloakId(jwt.getSubject()).orElseThrow(() -> ApiError.notFound("Usuario no encontrado")));
    }

    public User getUser(Long userId) {
        return dispathUser(repository.findById(userId).orElseThrow(() -> ApiError.notFound("Usuario no encontrado")));
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

        var authoritiesToRemove = userDto.getAuthoritiesToRemove();
        var authoritiesToAdd = userDto.getAuthoritiesToAdd();

        keycloakUserManager.removeRoles(keycloakId, Authority.getAuthorities(authoritiesToRemove));
        keycloakUserManager.addRoles(keycloakId, Authority.getAuthorities(authoritiesToAdd));
    }

    @PreAuthorize("isAuthenticated()")
    public User profile(Jwt jwt) {
        return getUser(jwt);
    }

    @Override
    public List<User> search(String search) {
        var representations = keycloakUserManager.search(search);

        return representations.stream()
                .map(convertRepresentationToUser(representations))
                .toList();
    }

    @Override
    public Page<User> search(String search, Pageable pageable) {
        var representations = keycloakUserManager.search(search, pageable);
        return representations.map(convertRepresentationToUser(representations.toList()));
    }

    public List<User> listByRole(String search, Authority role) {
        var representations = findRepresentations(search, role);

        return representations.stream()
                .map(convertRepresentationToUser(representations))
                .toList();
    }

    public Page<User> pageByRole(String search, Authority role, Pageable pageable) {
        var representations = findRepresentations(search, role);

        var result = sortRepresentations(representations.stream(), pageable)
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .map(convertRepresentationToUser(representations))
                .toList();

        return PageableExecutionUtils.getPage(result, pageable, representations::size);
    }

    private List<Comparator<UserRepresentation>> getSortComparators(Pageable pageable) {
        var sort = pageable.getSort();
        return sort.stream()
                .map(order -> switch (order.getProperty()) {
                    case "email" -> Comparator.comparing(UserRepresentation::getEmail);
                    case "username" -> Comparator.comparing(UserRepresentation::getUsername);
                    case "firstName" -> Comparator.comparing(UserRepresentation::getFirstName);
                    case "lastName" -> Comparator.comparing(UserRepresentation::getLastName);
                    default -> null;
                })
                .filter(Objects::nonNull)
                .toList();
    }


    private User dispathUser(User user) {
        var representation = keycloakUserManager.get(user.getKeycloakId());
        return dispathUser(user, representation);
    }

    private User dispathUser(User user, UserRepresentation representation) {
        Assert.isTrue(user.getKeycloakId().equals(representation.getId()), "User keycloak id does not match with representation id");

        var authorities = authorityExtractor.extract(representation);

        user.setRepresentation(representation);
        user.setAuthorities(authorities);
        return user;
    }

    private List<User> findUsers(List<UserRepresentation> representations) {
        var keycloakIds = extractKeycloakIds(representations);
        return repository.findAllByKeycloakIdIn(keycloakIds);
    }

    private Function<UserRepresentation, User> convertRepresentationToUser(List<UserRepresentation> representations) {
        var users = findUsers(representations);
        return representation -> {
            var user = users.stream()
                    .filter(u -> u.getKeycloakId().equals(representation.getId()))
                    .findFirst()
                    .orElseThrow(() -> ApiError.internalServerError("Inconsistencia de datos, usuario no encontrado. Keycloak id: " + representation.getId()));
            return dispathUser(user, representation);
        };
    }

    private List<UserRepresentation> findRepresentations(String search, Authority role) {
        var representations = findRepresentations(role);
        return search == null ? representations : representations
                .stream()
                .filter(filterRepresentations(search))
                .toList();
    }

    private List<UserRepresentation> findRepresentations(Authority role) {
        return keycloakUserManager.searchByRole(role.getAuthority());
    }

    private static List<String> extractKeycloakIds(List<UserRepresentation> representations) {
        return representations.stream()
                .map(UserRepresentation::getId)
                .toList();
    }

    private Stream<UserRepresentation> sortRepresentations(Stream<UserRepresentation> representations, Pageable pageable) {
        var sort = pageable.getSort();
        if (sort.isSorted()) {
            var comparators = getSortComparators(pageable);
            if (!comparators.isEmpty()) {
                representations = representations.sorted((a, b) -> {
                    for (var comparator : comparators) {
                        var result = comparator.compare(a, b);
                        if (result != 0) return result;
                    }
                    return 0;
                });
            }
        }
        return representations;
    }

    private static Predicate<UserRepresentation> filterRepresentations(String search) {
        return representation -> {
            var searchLowerCase = search.toLowerCase();
            var email = representation.getEmail().toLowerCase();
            var username = representation.getUsername().toLowerCase();
            var firstName = representation.getFirstName().toLowerCase();
            var lastName = representation.getLastName().toLowerCase();
            return email.contains(searchLowerCase)
                    || username.contains(searchLowerCase)
                    || firstName.contains(searchLowerCase)
                    || lastName.contains(searchLowerCase);
        };
    }
}