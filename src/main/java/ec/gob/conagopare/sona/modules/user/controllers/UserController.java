package ec.gob.conagopare.sona.modules.user.controllers;

import ec.gob.conagopare.sona.application.common.schemas.Message;
import ec.gob.conagopare.sona.application.common.utils.ResponseEntityUtils;
import ec.gob.conagopare.sona.modules.user.dto.UserDto;
import ec.gob.conagopare.sona.modules.user.dto.SingUpUser;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import ec.gob.conagopare.sona.modules.user.models.User;
import ec.gob.conagopare.sona.modules.user.service.UserService;
import io.github.luidmidev.springframework.data.crud.core.controllers.CrudController;
import io.github.luidmidev.springframework.data.crud.core.utils.PageableUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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

    @GetMapping("/profile-picture")
    public ResponseEntity<ByteArrayResource> profilePicture(@AuthenticationPrincipal Jwt jwt) {
        var stored = service.profilePicture(jwt);
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

    @GetMapping("/role/{role}")
    public ResponseEntity<List<User>> listByRole(
            @RequestParam(required = false) String search,
            @PathVariable Authority role
    ) {
        return ResponseEntity.ok(service.listByRole(search, role));
    }

    @GetMapping("/role/{role}/page")
    public ResponseEntity<Page<User>> pageByRole(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false) String[] properties,
            @RequestParam(required = false) Sort.Direction direction,
            @PathVariable Authority role
    ) {
        var pageable = PageableUtils.resolvePage(size, page, direction, properties);
        return ResponseEntity.ok(service.pageByRole(search, role, pageable));
    }
}
