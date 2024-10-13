package ec.gob.conagopare.sona.services;


import com.ketoru.springframework.email.EmailDetails;
import com.ketoru.springframework.email.EmailSenderService;
import com.ketoru.springframework.errors.ApiError;
import com.ketoru.store.core.FileStore;
import com.ketoru.validations.FileContentType;
import ec.gob.conagopare.sona.dto.Login;
import ec.gob.conagopare.sona.dto.RecoveryPasswordData;
import ec.gob.conagopare.sona.dto.Register;
import ec.gob.conagopare.sona.dto.TokenContainer;
import ec.gob.conagopare.sona.models.TokenForgetPassword;
import ec.gob.conagopare.sona.models.User;
import ec.gob.conagopare.sona.repositories.AuthorityRepository;
import ec.gob.conagopare.sona.repositories.TokenForgetPasswordRepository;
import ec.gob.conagopare.sona.repositories.UserRepository;
import ec.gob.conagopare.sona.security.jwt.JwtService;
import ec.gob.conagopare.sona.utils.MessageResolverI18n;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Random;


@Transactional
@Log4j2
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final String ROLE_USER = "ROLE_USER";

    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final TokenForgetPasswordRepository forgetPasswordRepository;
    private final FileStore storage;
    private final DaoAuthenticationProvider authenticationProvider;
    private final EmailSenderService emailSender;
    private final PasswordEncoder encoder;
    private final MessageResolverI18n resolver;
    private final JwtService jwtService;


    @PostConstruct
    public void init() {
        log.info("AuthenticationService initialized");
        this.forgetPasswordRepository.deleteExpiredToken();
    }

    public TokenContainer login(Login login) {
        log.info("Login: {}", login);
        var authenticationToken = UsernamePasswordAuthenticationToken.unauthenticated(login.getUsername().trim(), login.getPassword());
        var authentication = authenticationProvider.authenticate(authenticationToken);
        var user = (User) authentication.getPrincipal();

        var token = this.jwtService.create(user.getId().toString(), user.getUsername());
        return new TokenContainer(token);
    }

    public TokenContainer register(

            @Valid Register register,

            @FileContentType({"image/jpeg", "image/png"})
            @NotNull
            MultipartFile profilePicture

    ) throws IOException {

        if (userRepository.existsByUsername(register.getUsername())) {
            throw ApiError.notFound(resolver.get("user.username-exists"));
        }

        if (userRepository.existsByEmail(register.getEmail())) {
            throw ApiError.notFound(resolver.get("user.email-address-exists"));
        }


        var userRole = authorityRepository.findByName(ROLE_USER).orElseThrow(() -> ApiError.notFound(resolver.get("user.role-not-found", ROLE_USER)));

        var identifier = storage.store(profilePicture.getInputStream(), profilePicture.getOriginalFilename(), "/profile_photos");

        var user = User.builder()
                .username(register.getUsername())
                .password(encoder.encode(register.getPassword()))
                .name(register.getName())
                .lastname(register.getLastname())
                .email(register.getEmail().trim())
                .enabled(true)
                .accountNonLocked(true)
                .authorities(List.of(userRole))
                .profilePicture(identifier)
                .build();

        var saved = userRepository.save(user);
        emailSender.sendSimpleMail(register.getEmail(), EmailDetails.builder()
                .subject("Registro exitoso")
                .content("Bienvenido querido usuario")
                .build()
        );

        var token = this.jwtService.create(saved.getId().toString(), saved.getUsername());
        return new TokenContainer(token);
    }


    public void logout(@NotNull String token) {
        jwtService.revoke(token.replace("Bearer ", ""));
    }


    public void forgotPassword(String email) {

        if (userRepository.existsByEmail(email)) {
            throw ApiError.notFound(userNotFound());
        }

        var token = generateCode();
        saveTokenForgetPassword(email, token);
        emailSender.sendSimpleMail(email, "Código de recuperación de contraseña", "Su código de recuperación es: " + token);
    }


    public void resetPassword(RecoveryPasswordData data) {

        var tokenOptional = forgetPasswordRepository.findByToken(data.getRecoveryToken());

        if (tokenOptional.isEmpty()) {
            throw ApiError.badRequest(resolver.get("authentication.invalid-recovery-token"));
        }

        var token = tokenOptional.get();

        if (token.isExpired()) {
            forgetPasswordRepository.delete(token);
            throw ApiError.badRequest(resolver.get("authentication.expired-recovery-token"));
        }

        var userOptional = userRepository.findByEmail(token.getEmail());

        if (userOptional.isEmpty()) {
            throw ApiError.badRequest(userNotFound());
        }

        var user = userOptional.get();

        user.setPassword(encoder.encode(data.getNewPassword()));
        userRepository.save(user);
        forgetPasswordRepository.delete(token);
    }

    private void saveTokenForgetPassword(String email, String token) {

        var tokenForgetPassword = TokenForgetPassword.builder()
                .email(email)
                .token(token)
                .build();

        forgetPasswordRepository.save(tokenForgetPassword);
    }


    private static final Random RANDOM = new Random();

    private static String generateCode() {
        return String.format("%08d", RANDOM.nextInt(100000000));
    }

    private String userNotFound() {
        return resolver.get("authentication.user-not-found", "email");
    }
}

