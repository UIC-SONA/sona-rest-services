package ec.gob.conagopare.sona.modules.user.controllers;

import ec.gob.conagopare.sona.application.common.schemas.Message;
import ec.gob.conagopare.sona.application.common.utils.ResponseEntityUtils;
import ec.gob.conagopare.sona.modules.user.dto.ProfessionalScheduleDto;
import ec.gob.conagopare.sona.modules.user.dto.UserDto;
import ec.gob.conagopare.sona.modules.user.dto.SingUpUser;
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

    @GetMapping("/profile-picture")
    public ResponseEntity<ByteArrayResource> profilePicture(@AuthenticationPrincipal Jwt jwt) {
        var stored = service.profilePicture(jwt);
        return ResponseEntityUtils.resource(stored);
    }

    @GetMapping("{id}/profile-picture")
    public ResponseEntity<ByteArrayResource> profilePicture(@PathVariable Long id) {
        var stored = service.profilePicture(id);
        return ResponseEntityUtils.resource(stored);
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
}
