package ec.gob.conagopare.sona.modules.user.controllers;


import ec.gob.conagopare.sona.application.common.schemas.Message;
import ec.gob.conagopare.sona.application.common.utils.ResponseEntityUtils;
import ec.gob.conagopare.sona.modules.user.dto.OnboardUser;
import ec.gob.conagopare.sona.modules.user.dto.SignupUser;
import ec.gob.conagopare.sona.modules.user.dto.UpdateUser;
import ec.gob.conagopare.sona.modules.user.entities.User;
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
    public ResponseEntity<Message> signup(
            @RequestBody SignupUser signupUser
    ) {
        service.signup(signupUser);
        return ResponseEntity.ok(new Message("Usuario creado correctamente"));
    }


    @PreAuthorize("isAuthenticated()")
    @PostMapping("/onboard")
    public ResponseEntity<User> onboard(
            @RequestBody OnboardUser dto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(service.onboard(dto, jwt));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/profile")
    public ResponseEntity<User> update(
            @RequestBody UpdateUser dto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(service.update(dto, jwt));
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
            @RequestParam("photo") MultipartFile photo,
            @AuthenticationPrincipal Jwt jwt
    ) throws IOException {
        service.uploadProfilePicture(photo, jwt);
        return ResponseEntity.ok(new Message("Profile photo updated"));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/profile-picture")
    public ResponseEntity<Message> deleteProfilePicture(
            @AuthenticationPrincipal Jwt jwt
    ) {
        service.deleteProfilePicture(jwt);
        return ResponseEntity.ok(new Message("Profile photo deleted"));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/has-onboarded")
    public ResponseEntity<Boolean> hasOnboarded(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.hasOnboarded(jwt));
    }
}
