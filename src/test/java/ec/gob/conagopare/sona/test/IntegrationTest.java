package ec.gob.conagopare.sona.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import ec.gob.conagopare.sona.modules.user.dto.SingUpUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ActiveProfiles("test")
public abstract class IntegrationTest {

    @Autowired
    protected ObjectMapper objectMapper;

    @Value("${user.bootstrap.admin.username}")
    private String adminUsername;

    @Value("${user.bootstrap.admin.password}")
    private String adminPassword;

    private static final RestTemplate REST_TEMPLATE = new RestTemplate();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        log.info("Registering properties via DynamicPropertyRegistry");

        // Propiedades de Keycloak
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> SharedContainers.KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + SharedContainers.getRealmName());
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> SharedContainers.KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + SharedContainers.getRealmName() + "/protocol/openid-connect/certs");
        registry.add("keycloak.client-id", SharedContainers::getClientId);
        registry.add("keycloak.cli.server-url", SharedContainers.KEYCLOAK_CONTAINER::getAuthServerUrl);
        registry.add("keycloak.cli.realm-master", () -> "master");
        registry.add("keycloak.cli.user", SharedContainers::getAdminUsername);
        registry.add("keycloak.cli.password", SharedContainers::getAdminPassword);
        registry.add("keycloak.cli.default-client.realm", SharedContainers::getRealmName);
        registry.add("user.sync-api-key", SharedContainers::getKcUserSyncApiKey);

        // Propiedades de Postgres
        registry.add("spring.datasource.url", SharedContainers.POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", SharedContainers.POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", SharedContainers.POSTGRES_CONTAINER::getPassword);

        // Propiedades de MongoDB
        registry.add("spring.data.mongodb.uri", SharedContainers.MONGO_CONTAINER::getReplicaSetUrl);
    }

    public String obtainAccessToken(String usernameOrEmail, String password) {
        var tokenUrl = SharedContainers.KEYCLOAK_CONTAINER.getAuthServerUrl() + "/realms/" + SharedContainers.getRealmName() + "/protocol/openid-connect/token";

        var headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");

        var body = new LinkedMultiValueMap<String, String>();
        body.add("client_id", SharedContainers.getClientId());
        body.add("client_secret", "MY-SECRET");
        body.add("username", usernameOrEmail);
        body.add("password", password);
        body.add("grant_type", "password");

        var request = new HttpEntity<>(body, headers);

        var response = REST_TEMPLATE.exchange(tokenUrl, HttpMethod.POST, request, String.class);

        try {
            var jsonNode = objectMapper.readTree(response.getBody());
            return jsonNode.get("access_token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Error parsing access token", e);
        }
    }

    protected void singUp(SingUpUser singUpUser, MockMvc mockMvc) throws Exception {
        var jsonRequest1 = objectMapper.writeValueAsString(singUpUser);

        mockMvc.perform(post("/user/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest1));
    }

    public final Credentials getAdminCredentials() {
        return new Credentials(adminUsername, adminPassword);
    }

    public final String obtainAdminBearerToken() {
        var credentials = getAdminCredentials();
        return "Bearer " + obtainAccessToken(credentials.username, credentials.password);
    }

    protected Integer getUserId(String accessToken, MockMvc mockMvc) throws Exception {
        return JsonPath.read(mockMvc.perform(get("/user/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    public record Credentials(String username, String password) {
    }
}