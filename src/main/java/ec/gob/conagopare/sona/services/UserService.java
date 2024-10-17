package ec.gob.conagopare.sona.services;


import com.ketoru.springframework.errors.ApiError;
import com.ketoru.store.core.FileStore;
import com.ketoru.validations.FileContentType;
import ec.gob.conagopare.sona.dto.UpdateUser;
import ec.gob.conagopare.sona.dto.UpdateUserFromAdmin;
import ec.gob.conagopare.sona.models.Authority;
import ec.gob.conagopare.sona.models.User;
import ec.gob.conagopare.sona.repositories.AuthorityRepository;
import ec.gob.conagopare.sona.repositories.UserRepository;
import ec.gob.conagopare.sona.utils.MessageAccessor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Implementación de la interfaz UserDetailsService de Spring Security que carga los detalles de un usuario.
 * Esta implementación utiliza UserRepository para obtener los detalles del usuario desde una fuente de datos.
 */
@Transactional
@Validated
@Log4j2
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {


    private final UserRepository repository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder encoder;
    private final FileStore storage;
    private final MessageAccessor messages;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessResourceFailureException {
        return repository.findAllByUsernameOrEmail(username, username)
                .stream()
                .findFirst()
                .orElseThrow(() -> new UsernameNotFoundException(this.messages.getMessage("JdbcDaoImpl.notFound", new Object[]{username}, "User {0} not found")));
    }


    public List<User> list() {
        return repository.findAll();
    }

    public User find(UUID id) {
        return repository.findById(id).orElseThrow(() -> ApiError.badRequest(messages.getMessage("user.not-found")));
    }

    public void updateDetails(UUID id, @NotNull UpdateUserFromAdmin detailsUser) {

        var authorities = detailsUser.getAuthorities();
        var enabled = detailsUser.getEnabled();
        var locked = detailsUser.getLocked();
        var password = detailsUser.getPassword();

        if (authorities == null && enabled == null && password == null && locked == null) {
            throw ApiError.badRequest(messages.getMessage("user.no-data-to-update"));
        }

        var user = find(id);

        if (authorities != null) {
            var authoritiesAvailable = authorityRepository.findAllByNameIn(authorities);
            validateAuthorities(authoritiesAvailable, authorities);
            user.setAuthorities(authoritiesAvailable);
        }

        if (enabled != null) user.setEnabled(enabled);
        if (locked != null) user.setAccountNonLocked(!locked);
        if (password != null && !password.isBlank()) user.setPassword(encoder.encode(password));

        repository.save(user);
    }


    public void update(
            User user,
            @Valid UpdateUser updateUser,
            @FileContentType({"image/jpeg", "image/png"}) MultipartFile profileImage
    ) throws IOException {

        if (updateUser == null && profileImage == null) {
            throw ApiError.badRequest(messages.getMessage("user.no-data-to-update"));
        }

        if (updateUser != null) {
            updateUserData(user, updateUser);
        }

        if (profileImage != null) {
            updateProfileImage(profileImage, user);
        }

        repository.save(user);
    }

    private void updateProfileImage(MultipartFile profileImage, User user) throws IOException {
        var fullPath = storage.store(profileImage.getInputStream(), profileImage.getOriginalFilename(), "/profile_photos");
        var oldFile = user.getProfilePicture();
        storage.remove(oldFile);
        user.setProfilePicture(fullPath);
    }

    private void updateUserData(User user, UpdateUser updateUser) {
        var username = updateUser.getUsername();

        if (username != null && !username.equals(user.getUsername()) && repository.existsByUsername(username)) {
            throw ApiError.badRequest(messages.getMessage("user.username-exists"));
        }

        if (username != null) user.setUsername(username);
        if (updateUser.getName() != null) user.setName(updateUser.getName());
        if (updateUser.getLastname() != null) user.setLastname(updateUser.getLastname());
        if (updateUser.getPassword() != null) user.setPassword(encoder.encode(updateUser.getPassword()));
    }

    private void validateAuthorities(List<Authority> authoritiesAvailable, List<String> authorities) {
        if (authoritiesAvailable.size() != authorities.size()) {
            var authorititesNotFound = authorities.stream().filter(filter -> authoritiesAvailable.stream().noneMatch(authority -> authority.is(filter))).toArray();
            throw ApiError.notFound(messages.getMessage("user.role-not-found", new Object[]{Arrays.toString(authorititesNotFound)}));
        }
    }

}

