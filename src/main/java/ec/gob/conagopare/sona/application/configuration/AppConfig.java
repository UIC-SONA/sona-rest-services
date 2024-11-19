package ec.gob.conagopare.sona.application.configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.StorageOptions;
import io.github.luidmidev.jakarta.validations.utils.LocaleContext;
import io.github.luidmidev.springframework.web.problemdetails.ApiError;
import io.github.luidmidev.storage.Storage;
import io.github.luidmidev.storage.google.cloud.GoogleCloudStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AppConfig {

    @EventListener(ApplicationStartedEvent.class)
    public void onApplicationStarted() {
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
    public Storage storage() throws IOException {
        try (var resourceCredentials = new FileInputStream("google/service_account_storage.json")) {
            var storage = StorageOptions.http()
                    .setCredentials(GoogleCredentials.fromStream(resourceCredentials))
                    .build()
                    .getService();

            var bucket = storage.get("sona_app");
            return new GoogleCloudStorage(bucket);
        }
    }

    /**
     * Crear un bean para manejar las peticiones http mediante el cliente RestTemplate
     *
     * @return RestTemplate configurado para manejar errores de respuesta
     */
    @Bean
    public RestTemplate restTemplate() {
        log.info("Configuring rest template for http client");

        var client = new RestTemplate();
        client.setErrorHandler(new ResponseErrorHandler() {

            @Override
            public boolean hasError(@NotNull ClientHttpResponse response) throws IOException {
                var status = response.getStatusCode();
                return status.isError() || status.is5xxServerError() || status.is4xxClientError();
            }

            @Override
            public void handleError(@NotNull ClientHttpResponse response) throws IOException {
                var bodyAsString = new String(response.getBody().readAllBytes());
                log.info("Error response on web client: {}", bodyAsString);
                throw ApiError.status(response.getStatusCode()).detail(
                        "Request Error, info: [\n" +
                                " url: " + response.getHeaders().getLocation() + "\n" +
                                " response: " + bodyAsString + "\n" +
                                " status: " + response.getStatusCode() + "\n" +
                                "]"
                );
            }
        });
        return client;
    }
}
