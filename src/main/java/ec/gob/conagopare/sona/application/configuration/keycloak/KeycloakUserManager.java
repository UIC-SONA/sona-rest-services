package ec.gob.conagopare.sona.application.configuration.keycloak;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *  Removed for coverage testing:
 *  :
 *  public Page<UserRepresentation> search(String search, Pageable pageable) {
 *         if (pageable.isUnpaged()) {
 *             return PageableExecutionUtils.getPage(
 *                     search == null ? cli.users().list() : cli.users().search(search),
 *                     pageable,
 *                     () -> cli.users().count(search)
 *             );
 *         }
 *         var first = pageable.getPageNumber() * pageable.getPageSize();
 *         var max = pageable.getPageSize();
 *         var result = search == null ? cli.users().list(first, max) : cli.users().search(search, first, max);
 *         return PageableExecutionUtils.getPage(
 *                 result,
 *                 pageable,
 *                 () -> cli.users().count(search)
 *         );
 *     }
 *     public List<RoleRepresentation> userRoles(String userId) {
 *         return cli.users()
 *                 .get(userId)
 *                 .roles()
 *                 .clientLevel(cli.getClientUiid())
 *                 .listAll();
 *     }
 *     public List<UserRepresentation> searchByRole(String roleName) {
 *         return cli.client().roles().get(roleName).getUserMembers();
 *     }
 *     public List<UserRepresentation> searchByAttributes(String query) {
 *         return cli.users().searchByAttributes(query);
 *     }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakUserManager {

    private final KeycloakClientManager cli;


    public UserRepresentation get(String id) {
        return cli.users().get(id).toRepresentation();
    }

    /**
     * Find a user by username
     *
     * @param username the username to search
     * @return an optional with the user if found
     */
    public Optional<UserRepresentation> searchByUsername(String username) {
        return searchByUsername(username, true).stream().findFirst();
    }

    /**
     * Search users by username
     * @param username the username to search
     * @param exact if the search should be exact
     * @return a list with the users found
     */
    public List<UserRepresentation> searchByUsername(String username, boolean exact) {
        return cli.users().searchByUsername(username, exact);
    }

    /**
     * Get all users
     *
     * @return a list with all users
     */
    public List<UserRepresentation> list() {
        return cli.users().list();
    }


    /**
     * Search users
     * @param search the search query
     * @return a list with the users found
     */
    public List<UserRepresentation> search(String search) {
        return cli.users().search(search, 0, Integer.MAX_VALUE);
    }

    /**
     * Find a user by email
     *
     * @param email the email to search
     * @return an optional with the user if found
     * @see #searchByEmail(String, boolean)
     */
    public Optional<UserRepresentation> searchByEmail(String email) {
        return searchByEmail(email, true).stream().findFirst();
    }

    /**
     * Search users by email
     *
     * @param email the email to search
     * @param exact if the search should be exact
     * @return a list with the users found
     */
    public List<UserRepresentation> searchByEmail(String email, boolean exact) {
        return cli.users().searchByEmail(email, exact);
    }

    /**
     * Change enabled status
     * @param userId the user id
     * @param enabled the new status
     */
    public void enabled(String userId, boolean enabled) {
        var user = cli.users().get(userId);
        var representation = user.toRepresentation();
        representation.setEnabled(enabled);
        user.update(representation);
    }

    public String create(UserRepresentation newUser) {

        newUser.setEnabled(true);
        newUser.setEmailVerified(true);

        try (var response = cli.users().create(newUser)) {

            var status = response.getStatus();
            if (status == 409) {
                throw new ResponseStatusException(HttpStatusCode.valueOf(status), "User already exists " + newUser.getUsername());
            }

            if (status != 201) {
                var error = response.readEntity(String.class);
                throw new ResponseStatusException(HttpStatusCode.valueOf(status), "Error creating user " + newUser.getUsername() + " " + error);
            }

            return extractUserId(response);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating user", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating user");
        }
    }

    public void update(String keycloakId, Consumer<UserRepresentation> consumer) {
        var user = cli.users().get(keycloakId);
        var representation = user.toRepresentation();
        consumer.accept(representation);
        user.update(representation);
    }

    public void resetPassword(String userId, String password) {
        var user = cli.users().get(userId);
        user.resetPassword(getPasswordCredentials(password));
    }

    public void addRoles(String userId, String... roles) {
        var roleRepresentations = getRoles(roles);
        addRoles(userId, roleRepresentations);
    }

    public void addRoles(String userId, RoleRepresentation... roles) {
        var user = cli.users().get(userId);
        actionRoles(user, RoleScopeResource::add, roles);
    }

    public void removeRoles(String userId, String... roles) {
        var roleRepresentations = getRoles(roles);
        removeRoles(userId, roleRepresentations);
    }

    public void removeRoles(String userId, RoleRepresentation... roles) {
        var user = cli.users().get(userId);
        actionRoles(user, RoleScopeResource::remove, roles);
    }


    private void actionRoles(UserResource user, BiConsumer<RoleScopeResource, List<RoleRepresentation>> action, RoleRepresentation... roles) {
        if (roles.length == 0) return;
        var roleScopeResource = user.roles().clientLevel(cli.getClientUiid());
        action.accept(roleScopeResource, Arrays.asList(roles));
    }

    private RoleRepresentation[] getRoles(String... roleNames) {
        return Stream.of(roleNames).map(this::searchRole).toArray(RoleRepresentation[]::new);
    }

    private RoleRepresentation searchRole(String roleName) {
        log.info("Search role {}", roleName);
        return cli.roles().get(roleName).toRepresentation();
    }

    private CredentialRepresentation getPasswordCredentials(String password) {
        return getCredentials(CredentialRepresentation.PASSWORD, password);
    }

    private CredentialRepresentation getCredentials(String type, String value) {
        return getCredentials(type, value, false);
    }

    private CredentialRepresentation getCredentials(String type, String value, boolean temporary) {
        var credentials = new CredentialRepresentation();
        credentials.setType(type);
        credentials.setValue(value);
        credentials.setTemporary(temporary);
        return credentials;
    }


    public void delete(String id) {
        try (var response = cli.users().delete(id)) {
            log.debug("Delete user, response: {}", response);
        }
    }

    private static String extractUserId(Response response) {
        var path = response.getLocation().getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    public void sendEmailResetPassword(String userId) {
        cli.users().get(userId).executeActionsEmail(List.of("UPDATE_PASSWORD"));
    }

}
