package ec.gob.conagopare.sona.services;

import com.ketoru.springframework.errors.ApiError;
import ec.gob.conagopare.sona.models.RecoveryPasswordCode;
import ec.gob.conagopare.sona.repositories.RecoveryPasswordCodeRepository;
import ec.gob.conagopare.sona.security.WebSecurityProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class RecoveryPasswordCodeService implements MessageSourceAware {

    private static final Random RANDOM = new Random();
    private final RecoveryPasswordCodeRepository repository;
    private final WebSecurityProperties properties;
    private long recoveryPasswordCodeExpirationTimeMinutes;
    private MessageSourceAccessor messageSource;


    @PostConstruct
    public void init() {
        recoveryPasswordCodeExpirationTimeMinutes = properties.getAuthentication().getRecoveryPasswordCodeExpirationDuration().toMinutes();
        removeExpiredTokens();
    }

    public RecoveryPasswordCode createRecoveryCode(String email) {

        var code = generateCode();

        var tokenForgetPassword = RecoveryPasswordCode.builder()
                .email(email)
                .code(code)
                .createdAt(LocalDateTime.now())
                .build();

        return repository.save(tokenForgetPassword);
    }


    private static String generateCode() {
        return String.format("%08d", RANDOM.nextInt(100000000));
    }


    @Scheduled(cron = "0 0 0 * * *")
    private void removeExpiredTokens() {
        repository.deleteExpiredTokens(recoveryPasswordCodeExpirationTimeMinutes);
    }

    /**
     * Use the recovery code to recover the password, this code is deleted after being used.
     *
     * @param recoveryCode the recovery code
     * @param consumer the consumer to execute if the code is valid
     */
    public void useCode(String recoveryCode, Consumer<RecoveryPasswordCode> consumer) {

        var code = repository.findByCode(recoveryCode).orElseThrow(() -> ApiError.badRequest(messageSource.getMessage("authentication.invalid-recovery-token")));

        if (code.isExpired(recoveryPasswordCodeExpirationTimeMinutes)) {
            throw ApiError.badRequest(messageSource.getMessage("authentication.expired-recovery-token"));
        }
        consumer.accept(code);
        repository.delete(code);
    }

    @Override
    public void setMessageSource(@NotNull MessageSource messageSource) {
        this.messageSource = new MessageSourceAccessor(messageSource);
    }
}
