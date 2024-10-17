package ec.gob.conagopare.sona.controllers;


import ec.gob.conagopare.sona.dto.UpdateUser;
import ec.gob.conagopare.sona.dto.UpdateUserFromAdmin;
import ec.gob.conagopare.sona.models.User;
import ec.gob.conagopare.sona.services.UserService;
import ec.gob.conagopare.sona.utils.MessageAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;


@Log4j2
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;
    private final MessageAccessor messages;

    @PutMapping("/update-profile")
    public ResponseEntity<String> update(
            @AuthenticationPrincipal User principal,
            @RequestPart(required = false) UpdateUser data,
            @RequestPart(required = false) MultipartFile profileImage
    ) throws IOException {
        service.update(principal, data, profileImage);
        return ResponseEntity.ok(messages.getMessage("user.updated"));
    }

    @GetMapping
    public ResponseEntity<Iterable<User>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> find(@PathVariable UUID id) {
        return ResponseEntity.ok(service.find(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateDetails(
            @PathVariable UUID id,
            @RequestBody UpdateUserFromAdmin detailsUser
    ) {
        service.updateDetails(id, detailsUser);
        return ResponseEntity.ok(messages.getMessage("user.updated"));
    }
}