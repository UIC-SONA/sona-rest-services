package ec.gob.conagopare.sona.services;


import com.ketoru.springframework.email.EmailSenderService;
import com.ketoru.springframework.errors.ApiError;
import com.ketoru.store.core.FileStore;
import com.ketoru.validations.FileContentType;
import ec.gob.conagopare.sona.dto.Login;
import ec.gob.conagopare.sona.dto.RecoveryPasswordData;
import ec.gob.conagopare.sona.dto.Register;
import ec.gob.conagopare.sona.dto.TokenContainer;
import ec.gob.conagopare.sona.models.User;
import ec.gob.conagopare.sona.repositories.AuthorityRepository;
import ec.gob.conagopare.sona.repositories.UserRepository;
import ec.gob.conagopare.sona.security.jwt.Jwt;
import ec.gob.conagopare.sona.utils.MessageAccessor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


@Transactional
@Log4j2
@Validated
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final String ROLE_USER = "ROLE_USER";

    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder encoder;
    private final FileStore storage;
    private final MessageAccessor messages;
    private final RecoveryPasswordCodeService recoveryPasswordCodeService;
    private final DaoAuthenticationProvider authenticationProvider;
    private final EmailSenderService emailSender;
    private final Jwt jwt;

    public TokenContainer login(@Valid Login login) {
        var authenticationToken = UsernamePasswordAuthenticationToken.unauthenticated(login.getUsername().trim(), login.getPassword());
        var authentication = authenticationProvider.authenticate(authenticationToken);
        var user = (User) authentication.getPrincipal();

        var token = this.jwt.create(user.getId().toString(), user.getUsername());
        return new TokenContainer(token);
    }

    public TokenContainer register(
            @Valid Register register,
            @FileContentType({"image/jpeg", "image/png"}) MultipartFile profilePicture
    ) throws IOException {

        var username = register.getUsername().trim();
        var email = register.getEmail().trim();

        if (userRepository.existsByUsername(username)) {
            throw ApiError.notFound(messages.getMessage("user.username-exists"));
        }

        if (userRepository.existsByEmail(email)) {
            throw ApiError.notFound(messages.getMessage("user.email-address-exists"));
        }

        var userRole = authorityRepository.findByName(ROLE_USER).orElseThrow(() -> ApiError.notFound(messages.getMessage("user.role-not-found", new Object[]{ROLE_USER})));

        var user = User.builder()
                .username(username)
                .password(encoder.encode(register.getPassword()))
                .email(email)
                .name(register.getName().trim())
                .lastname(register.getLastname().trim())
                .enabled(true)
                .accountNonLocked(true)
                .authorities(List.of(userRole))
                .build();

        if (profilePicture != null) {
            var identifier = storage.store(profilePicture.getInputStream(), profilePicture.getOriginalFilename(), "/profile_photos");
            user.setProfilePicture(identifier);
        }

        var saved = userRepository.save(user);
        emailSender.sendSimpleMail(register.getEmail(), "Registro exitoso", "Bienvenido querido usuario");

        var token = this.jwt.create(saved.getId().toString(), saved.getUsername());
        return new TokenContainer(token);
    }


    public void logout(String token) {
        jwt.revoke(token.replace("Bearer ", ""));
    }

    public void forgotPassword(String email) {

        if (userRepository.existsByEmail(email)) {
            throw ApiError.notFound(userNotFound());
        }

        var code = recoveryPasswordCodeService.createRecoveryCode(email);

        emailSender.sendSimpleMail(email, "Código de recuperación de contraseña", "Su código de recuperación es: " + code.getCode());
    }


    public void resetPassword(@Valid RecoveryPasswordData data) {
        recoveryPasswordCodeService.useCode(data.getRecoveryCode(), code -> {
            var user = userRepository.findByEmail(code.getEmail()).orElseThrow(() -> ApiError.notFound(userNotFound()));

            user.setPassword(encoder.encode(data.getNewPassword()));
            userRepository.save(user);

        });
    }

    private String userNotFound() {
        return messages.getMessage("authentication.user-not-found", new Object[]{"email"});
    }
}

