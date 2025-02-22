package ec.gob.conagopare.sona.application.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@ConditionalOnExpression("${springdoc.api-docs.enabled:false}")
public class OpenAPIConfiguration {

    private final String clientId;
    private final Map<String, ?> openIdConnectConfig;

    private static final String OAUTH2_SCHEME_NAME = "oauth2";

    public OpenAPIConfiguration(@Value("${openapi.openid-connect-url}") String openIdConnectUrl, @Value("${keycloak.client-id}") String clientId) {

        this.clientId = clientId;
        this.openIdConnectConfig = new RestTemplate().exchange(openIdConnectUrl, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, ?>>() {
        }).getBody();

        if (this.openIdConnectConfig == null) {
            throw new IllegalStateException("OpenID Connect configuration not found at " + openIdConnectUrl);
        }

        log.info("OpenID Connect configuration loaded from {}, {}", openIdConnectUrl, openIdConnectConfig);
    }

    /**
     * @return OpenAPI configuration for development environment
     */
    @Bean
    public OpenAPI openAPI() {
        log.info("Configuring OpenAPI documentation for development environment");
        return new OpenAPI()
                .info(new Info()
                        .title("SONA API Documentation")
                        .description("DOCUMENTATION FOR SONA")
                        .version("1.0.0")
                )
                .components(new Components()
                        .addSecuritySchemes(
                                OAUTH2_SCHEME_NAME,
                                new SecurityScheme()
                                        .description("OAuth2 authentication")
                                        .type(SecurityScheme.Type.OAUTH2)
                                        .flows(new OAuthFlows()
                                                .authorizationCode(new OAuthFlow()
                                                        .authorizationUrl((String) openIdConnectConfig.get("authorization_endpoint"))
                                                        .tokenUrl((String) openIdConnectConfig.get("token_endpoint"))
                                                        .refreshUrl((String) openIdConnectConfig.get("token_endpoint"))
                                                        .scopes(getScopesSupported())
                                                        .extensions(rapidocExtensions())
                                                )
                                        )
                        )
                )
                .addSecurityItem(new SecurityRequirement()
//                        .addList(OPEN_ID_SCHEME_NAME)
                                .addList(OAUTH2_SCHEME_NAME)
                );
    }

    private Scopes getScopesSupported() {
        return openIdConnectConfig.get("scopes_supported") instanceof List<?> scopesSupported
                ? scopesSupported
                .stream()
                .reduce(
                        new Scopes(),
                        (scopes, scope) -> scopes.addString(scope.toString(), scope.toString()),
                        (scopes1, scopes2) -> scopes1
                )
                : new Scopes();
    }

    private Map<String, Object> rapidocExtensions() {
        return Map.of(
                "x-client-id", clientId
        );
    }
}
