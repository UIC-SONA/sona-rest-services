package ec.gob.conagopare.sona.controllers;


import ec.gob.conagopare.sona.dto.Login;
import ec.gob.conagopare.sona.dto.RecoveryPasswordData;
import ec.gob.conagopare.sona.dto.Register;
import ec.gob.conagopare.sona.dto.TokenContainer;
import ec.gob.conagopare.sona.services.AuthenticationService;
import ec.gob.conagopare.sona.utils.MessageResolverI18n;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Controlador para la autenticación y gestión de usuarios.
 */
@Log4j2
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService service;
    private final MessageResolverI18n resolver;

    /**
     * Autentica un usuario.
     *
     * @param login El objeto LoginDTO que contiene las credenciales del usuario.
     * @return Una ResponseEntity que contiene el token JWT si la autenticación es exitosa, o un mensaje de error si las credenciales son inválidas.
     */
    @PostMapping("/login")
    public ResponseEntity<TokenContainer> login(@RequestBody Login login) {
        var tokenResponse = service.login(login);
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<TokenContainer> register(
            @RequestPart Register data,
            @RequestPart MultipartFile profileImage
    ) throws IOException {
        var tokenResponse = service.register(data, profileImage);
        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * Cierra la sesión de un usuario.
     * @param token El token de autorización.
     * @return Una ResponseEntity que contiene el mensaje "Cerrada la sesión" si el cierre de sesión es exitoso, o un mensaje de error si ocurre algún problema.
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String token) {
        service.logout(token);
        return ResponseEntity.ok(resolver.get("user.logout"));
    }

    /**
     * Elimina un usuario.
     * @param email El email del usuario a eliminar.
     * @return Una ResponseEntity que contiene el mensaje "Eliminado" si la eliminación es exitosa, o un mensaje de error si ocurre algún problema.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        service.forgotPassword(email);
        return ResponseEntity.ok(resolver.get("user.recovery-password-email-sent"));
    }


    /**
     * Actualiza la contraseña de un usuario.
     * @param data El objeto RecoveryPasswordData que contiene el email, la nueva contraseña y el token de recuperación.
     * @return Una ResponseEntity que contiene el mensaje "Se ha actualizado su contraseña" si la actualización es exitosa, o un mensaje de error si ocurre algún problema.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody RecoveryPasswordData data) {
        service.resetPassword(data);
        return ResponseEntity.ok(resolver.get("user.reset-password-updated"));
    }

    /**
     * Obtiene los detalles de un usuario.
     *
     * @param principals El objeto AuthenticationPrincipal que representa al usuario autenticado.
     * @return Una ResponseEntity que contiene el objeto User si el usuario está autenticado, o un mensaje de error si no es válido.
     */
    @GetMapping("/user-info")
    public ResponseEntity<Object> whoami(@AuthenticationPrincipal Object principals) {
        return ResponseEntity.ok(principals);
    }
}