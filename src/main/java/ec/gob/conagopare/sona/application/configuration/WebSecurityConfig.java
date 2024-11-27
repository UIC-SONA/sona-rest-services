package ec.gob.conagopare.sona.application.configuration;


import com.nimbusds.jose.shaded.gson.internal.LinkedTreeMap;
import ec.gob.conagopare.sona.application.common.utils.functions.Extractor;
import ec.gob.conagopare.sona.application.filters.AuthenticationMDCFilter;
import ec.gob.conagopare.sona.modules.user.models.Authority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Clase de configuración de seguridad para Spring Security.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver, CorsConfigurationSource corsConfigurationSource, AuthenticationMDCFilter authenticationMDCFilter) throws Exception {
        log.info("Configuring SecurityFilterChain");
        return http
                .csrf(CsrfConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest()
                        .permitAll()
                )
                .oauth2ResourceServer(server -> server
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.error("Authentication error", authException);
                            resolver.resolveException(request, response, null, authException);
                        })
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .addFilterAfter(authenticationMDCFilter, BearerTokenAuthenticationFilter.class)
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverterForKeycloak(@Value("${keycloak.client-id}") String clientId) {
        var jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(authorityConverter(clientId));
        return jwtAuthenticationConverter;
    }


    @SuppressWarnings("unchecked")
    public static Converter<Jwt, Collection<GrantedAuthority>> authorityConverter(String clientId) {
        return jwt -> {
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");

            var clientRoleMap = (LinkedTreeMap<String, List<String>>) resourceAccess.get(clientId);
            var clientRoles = new ArrayList<>(clientRoleMap.get("roles"));

            return clientRoles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toUnmodifiableList());
        };
    }

    @Bean
    public Extractor<UserRepresentation, Collection<Authority>> authorityExtractor(@Value("${keycloak.client-id}") String clientId) {
        return representation -> {
            log.info("Representation: {}", representation);
            var clientsRoles = representation.getClientRoles();
            if (clientsRoles == null) {
                return List.of();
            }

            var clientRoles = clientsRoles.get(clientId);
            return clientRoles.stream()
                    .filter(Authority::exists)
                    .map(Authority::valueOf)
                    .toList();
        };
    }
}