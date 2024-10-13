package ec.gob.conagopare.sona.controllers;


import ec.gob.conagopare.sona.dto.UpdateUser;
import ec.gob.conagopare.sona.dto.UpdateUserFromAdmin;
import ec.gob.conagopare.sona.models.User;
import ec.gob.conagopare.sona.services.UserService;
import ec.gob.conagopare.sona.utils.MessageResolverI18n;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@Log4j2
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;
    private final MessageResolverI18n resolver;

    @PutMapping("/update-profile")
    public ResponseEntity<String> update(@AuthenticationPrincipal User principal,
                                         @RequestPart(required = false) UpdateUser data,
                                         @RequestPart(required = false) MultipartFile profileImage
    ) throws IOException {
        service.update(data, profileImage, principal);
        return ResponseEntity.ok().body(resolver.get("user.updated"));
    }

    @GetMapping
    public ResponseEntity<Iterable<User>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> find(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateDetails(@PathVariable Long id,
                                                @RequestBody UpdateUserFromAdmin detailsUser
    ) {
        service.updateDetails(id, detailsUser);
        return ResponseEntity.ok(resolver.get("user.updated"));
    }
}