package ec.gob.conagopare.sona.modules.user.controllers;

import ec.gob.conagopare.sona.application.common.schemas.Message;
import ec.gob.conagopare.sona.application.common.utils.ResponseEntityUtils;
import ec.gob.conagopare.sona.modules.user.dto.UserDto;
import ec.gob.conagopare.sona.modules.user.dto.SingUpUser;
import ec.gob.conagopare.sona.modules.user.dto.KeycloakUserSync;
import ec.gob.conagopare.sona.modules.user.models.User;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.springframework.data.crud.core.controllers.CrudController;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Getter
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController implements CrudController<User, UserDto, Long, UserService> {

    private final UserService service;

    @PostMapping("/sign-up")
    public ResponseEntity<Message> signUp(@RequestBody SingUpUser singUpUser) {
        service.signUp(singUpUser);
        return ResponseEntity.ok(new Message("Usuario registrado correctamente"));
    }

    @GetMapping("/map")
    public ResponseEntity<Map<Long, User>> map(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(service.map(ids));
    }

    @PostMapping("/anonymize")
    public ResponseEntity<Message> anonymize(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false, defaultValue = "true") boolean anonymize
    ) {
        service.anonymize(jwt, anonymize);
        return ResponseEntity.ok(new Message("Estado de anonimato actualizado correctamente a " + anonymize));
    }

    @PutMapping("/password")
    public ResponseEntity<Message> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String newPassword
    ) {
        service.changePassword(jwt, newPassword);
        return ResponseEntity.ok(new Message("Contraseña actualizada correctamente"));
    }

    @PostMapping("/password-reset")
    public ResponseEntity<Message> resetPassword(
            @RequestParam String emailOrUsername
    ) {
        service.resetPassword(emailOrUsername);
        return ResponseEntity.ok(new Message("Se ha enviado un correo con las instrucciones para restablecer la contraseña"));
    }


    @GetMapping("/profile-picture")
    public ResponseEntity<ByteArrayResource> profilePicture(@AuthenticationPrincipal Jwt jwt) {
        var stored = service.profilePicture(jwt);
        return ResponseEntityUtils.resource(stored, true);
    }

    @GetMapping("{id}/profile-picture")
    public ResponseEntity<ByteArrayResource> profilePicture(@PathVariable long id) {
        var stored = service.profilePicture(id);
        return ResponseEntityUtils.resource(stored, true);
    }

    @PostMapping(value = "/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Message> uploadProfilePicture(
            @RequestPart MultipartFile photo,
            @AuthenticationPrincipal Jwt jwt
    ) throws IOException {
        service.uploadProfilePicture(photo, jwt);
        return ResponseEntity.ok(new Message("Foto de perfil establecida corrextamente"));
    }

    @DeleteMapping("/profile-picture")
    public ResponseEntity<Message> deleteProfilePicture(
            @AuthenticationPrincipal Jwt jwt
    ) {
        service.deleteProfilePicture(jwt);
        return ResponseEntity.ok(new Message("Foto de perfil eliminada correctamente"));
    }

    @GetMapping("/profile")
    public ResponseEntity<User> profile(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(service.profile(jwt));
    }

    @PutMapping("/enable")
    public ResponseEntity<Message> enable(
            @RequestParam long id,
            @RequestParam boolean value,
            @AuthenticationPrincipal Jwt jwt
    ) {
        service.enable(id, value, jwt);
        return ResponseEntity.ok(new Message("Usuario " + (value ? "habilitado" : "deshabilitado") + " correctamente"));
    }


    @PostMapping("/keycloak-sync")
    public ResponseEntity<Message> syncKeycloak(
            @RequestBody KeycloakUserSync userSync,
            @RequestHeader("X-Api-Key") String apiKey
    ) {
        service.syncKeycloak(userSync, apiKey);
        return ResponseEntity.ok(new Message("Sincronización con Keycloak realizada correctamente"));
    }
}
