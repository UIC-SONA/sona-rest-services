package ec.gob.conagopare.sona.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.HashMap;

@Log4j2
@Configuration
@RequiredArgsConstructor
public class SecurityBeans {

    private final WebSecurityProperties properties;

    @Bean
    public PasswordEncoder passwordEncoder() {
        var encodingId = "argon2";
        var encoders = new HashMap<String, PasswordEncoder>();
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        encoders.put("pbkdf2", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        encoders.put("scrypt", SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8());
        encoders.put("argon2", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        return new DelegatingPasswordEncoder(encodingId, encoders);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var corsProperties = properties.getCors();
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsProperties.toCorsConfiguration());
        return source;
    }

    @Bean
    public UserDetailsChecker userDetailsChecker() {

        var messages = SpringSecurityMessageSource.getAccessor();

        return user -> {

            if (!user.isEnabled()) {
                throw new DisabledException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.disabled", "User is disabled"));
            }

            if (!user.isAccountNonLocked()) {
                throw new LockedException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.locked", "User account is locked"));
            }

            if (!user.isAccountNonExpired()) {
                throw new AccountExpiredException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.expired", "User account has expired"));
            }

            if (!user.isCredentialsNonExpired()) {
                throw new CredentialsExpiredException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.credentialsExpired", "User credentials have expired"));
            }
        };
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        var manager = configuration.getAuthenticationManager();
        log.info("Setting up authentication manager: {}", manager.getClass().getName());
        if (manager instanceof ProviderManager providerManager) {
            var providers = providerManager.getProviders();
            for (var provider : providers) log.info("Authentication provider detected: {}", provider.getClass().getName());
        }
        return manager;
    }


    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(PasswordEncoder passwordEncoder, UserDetailsChecker userDetailsChecker, UserDetailsService userDetailsService) {
        var provider = new DaoAuthenticationProvider(passwordEncoder);
        provider.setUserDetailsService(userDetailsService);
        provider.setPostAuthenticationChecks(userDetailsChecker);
        provider.setPreAuthenticationChecks(user -> {
        });
        return provider;
    }
}
