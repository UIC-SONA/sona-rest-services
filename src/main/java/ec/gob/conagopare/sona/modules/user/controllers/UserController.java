package ec.gob.conagopare.sona.modules.user.controllers;

import ec.gob.conagopare.sona.application.common.schemas.Message;
import ec.gob.conagopare.sona.application.common.utils.ResponseEntityUtils;
import ec.gob.conagopare.sona.modules.user.dto.SingUpUser;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @PreAuthorize("permitAll()")
    @PostMapping("/sign-up")
    public ResponseEntity<Message> signUp(@RequestBody SingUpUser singUpUser) {
        service.signUp(singUpUser);
        return ResponseEntity.ok(new Message("Usuario registrado correctamente"));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/profile-picture")
    public ResponseEntity<ByteArrayResource> getProfilePicture(@AuthenticationPrincipal Jwt jwt) {
        var stored = service.getProfilePicture(jwt);
        return ResponseEntityUtils.resource(stored);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Message> uploadProfilePicture(
            @RequestPart MultipartFile photo,
            @AuthenticationPrincipal Jwt jwt
    ) throws IOException {
        service.uploadProfilePicture(photo, jwt);
        return ResponseEntity.ok(new Message("Foto de perfil establecida corrextamente"));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/profile-picture")
    public ResponseEntity<Message> deleteProfilePicture(
            @AuthenticationPrincipal Jwt jwt
    ) {
        service.deleteProfilePicture(jwt);
        return ResponseEntity.ok(new Message("Foto de perfil eliminada correctamente"));
    }

}
