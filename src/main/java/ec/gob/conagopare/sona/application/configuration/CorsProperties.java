package ec.gob.conagopare.sona.application.configuration;

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
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    private List<String> allowedOrigins = new ArrayList<>();
    private List<String> allowedOriginPatterns = new ArrayList<>();
    private List<String> allowedMethods = new ArrayList<>();
    private List<String> allowedHeaders = new ArrayList<>();
    private List<String> exposedHeaders = new ArrayList<>();
    private Boolean allowCredentials;
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration maxAge = Duration.ofSeconds(1800);

    public CorsConfiguration toCorsConfiguration() {
        if (CollectionUtils.isEmpty(this.allowedOrigins) && CollectionUtils.isEmpty(this.allowedOriginPatterns)) return null;
        var config = new CorsConfiguration();
        var map = PropertyMapper.get();
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
