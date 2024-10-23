package ec.gob.conagopare.sona.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "security")
public class WebSecurityProperties {

    private final AuthenticationProperties authentication = new AuthenticationProperties();
    private final JwtProperties jwt = new JwtProperties();
    private final CorsProperties cors = new CorsProperties();

    @Data
    public static class AuthenticationProperties {

        @DurationUnit(ChronoUnit.MINUTES)
        private Duration recoveryPasswordCodeExpirationDuration = Duration.ofMinutes(60);

    }

    @Data
    public static class JwtProperties {

        private String issuer;
        private String secretKey;
        @DurationUnit(ChronoUnit.MILLIS)
        private Duration expirationTime = Duration.ofHours(1);
    }

    @Data
    static class CorsProperties {
        private List<String> allowedOrigins = new ArrayList<>();
        private List<String> allowedOriginPatterns = new ArrayList<>();
        private List<String> allowedMethods = new ArrayList<>();
        private List<String> allowedHeaders = new ArrayList<>();
        private List<String> exposedHeaders = new ArrayList<>();
        private Boolean allowCredentials;
        @DurationUnit(ChronoUnit.SECONDS)
        private Duration maxAge = Duration.ofSeconds(1800);

        public CorsConfiguration toCorsConfiguration() {
            if (CollectionUtils.isEmpty(this.allowedOrigins) && CollectionUtils.isEmpty(this.allowedOriginPatterns)) {
                return null;
            }
            var map = PropertyMapper.get();
            var config = new CorsConfiguration();
            map.from(this::getAllowedOrigins).to(config::setAllowedOrigins);
            map.from(this::getAllowedOriginPatterns).to(config::setAllowedOriginPatterns);
            map.from(this::getAllowedHeaders).whenNot(CollectionUtils::isEmpty).to(config::setAllowedHeaders);
            map.from(this::getAllowedMethods).whenNot(CollectionUtils::isEmpty).to(config::setAllowedMethods);
            map.from(this::getExposedHeaders).whenNot(CollectionUtils::isEmpty).to(config::setExposedHeaders);
            map.from(this::getMaxAge).whenNonNull().as(Duration::getSeconds).to(config::setMaxAge);
            map.from(this::getAllowCredentials).whenNonNull().to(config::setAllowCredentials);
            return config;
        }
    }

}
