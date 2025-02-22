package ec.gob.conagopare.sona.application.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.StorageOptions;
import io.github.luidmidev.jakarta.validations.utils.LocaleContext;
import io.github.luidmidev.springframework.data.crud.core.http.export.SpreadSheetExporter;
import io.github.luidmidev.storage.Storage;
import io.github.luidmidev.storage.google.cloud.GoogleCloudStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableTransactionManagement
public class ApplicationConfiguration {

    @EventListener(ApplicationStartedEvent.class)
    public void startedEvent() {
        LocaleContext.setLocaleSupplier(LocaleContextHolder::getLocale);
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        log.info("Configuring CORS with properties {}", properties);
        var configuration = properties.toCorsConfiguration();
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public MessageSourceAccessor messageSourceAccessor(MessageSource messageSource) {
        return new MessageSourceAccessor(messageSource);
    }

    @Bean
    public Storage storage(Environment environment) throws IOException {

        log.info("Configuring Google Cloud Storage");
        try (var resourceCredentials = new FileInputStream("google/service_account_storage.json")) {
            var storage = StorageOptions.http()
                    .setCredentials(GoogleCredentials.fromStream(resourceCredentials))
                    .build()
                    .getService();
            return new GoogleCloudStorage(storage.get(
                    environment.acceptsProfiles(Profiles.of("test"))
                            ? "sona_app_test"
                            : "sona_app"
            ));
        }
    }

    @Bean
    public SpreadSheetExporter spreadSheetExporter(ObjectMapper mapper) {
        return new SpreadSheetExporter(mapper);
    }
}