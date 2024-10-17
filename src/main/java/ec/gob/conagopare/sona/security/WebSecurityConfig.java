package ec.gob.conagopare.sona.security;


import ec.gob.conagopare.sona.security.authentication.AuthenticationOncePerRequestFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

/**
 * Clase de configuración de seguridad para Spring Security.
 */
@Log4j2
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final AuthenticationOncePerRequestFilter authenticationFilter;


    /**
     * Configura las cadenas de filtros de seguridad para las solicitudes HTTP.
     *
     * @param http Objeto HttpSecurity para la configuración de seguridad.
     * @return Cadena de filtros de seguridad.
     * @throws Exception Si se produce un error durante la configuración.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UserDetailsService userDetailsService, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver, CorsConfigurationSource corsConfigurationSource) throws Exception {

        log.info("Configurando seguridad de la aplicación");
        http.headers(head -> head.frameOptions(FrameOptionsConfig::disable));

        log.info("Configurando CSRF");
        http.csrf(CsrfConfigurer::disable);

        log.info("Configurando CORS");
        http.cors(cors -> cors.configurationSource(corsConfigurationSource));

        log.info("Configurando autorización de solicitudes HTTP");
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        antMatcher(HttpMethod.GET, "/files/**")
                ).permitAll()
                .requestMatchers(
                        "/auth/register",
                        "/auth/login",
                        "/auth/forgot-password",
                        "/auth/reset-password",
                        "/v3/api-docs/**",
                        "/run/**"
                ).permitAll()
                .requestMatchers(
                        "/auth/user-info"
                ).authenticated()
                .requestMatchers(
                        "/sample-user-endpoint/**"
                ).hasRole("USER")
                .requestMatchers(
                        "/sample-admin-endpoint/**"
                ).hasRole("ADMIN")
                .anyRequest()
                .authenticated()

        ).exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint((request, response, authException) -> resolver.resolveException(request, response, null, authException)));

        log.info("Configurando autenticación");
        http.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        log.info("Configurando filtro de autenticación JWT");

        http.addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);

        http.userDetailsService(userDetailsService);

        log.info("Configurando filtro de autenticación básica");
        http.httpBasic(basic -> basic.authenticationEntryPoint((request, response, authException) -> resolver.resolveException(request, response, null, authException)));

        return http.build();
    }
}
