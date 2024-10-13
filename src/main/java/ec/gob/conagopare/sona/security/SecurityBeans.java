package ec.gob.conagopare.sona.security;

import ec.gob.conagopare.sona.utils.MessageResolverI18n;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;

import java.util.HashMap;

@Log4j2
@Configuration
public class SecurityBeans {
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
    public UserDetailsChecker userDetailsChecker(MessageResolverI18n resolver) {
        return user -> {

            if (!user.isEnabled()) {
                throw new DisabledException(resolver.get("authentication.account-disabled"));
            }

            if (!user.isAccountNonLocked()) {
                throw new LockedException(resolver.get("authentication.account-locked"));
            }

            if (!user.isAccountNonExpired()) {
                throw new AccountExpiredException(resolver.get("authentication.account-expired"));
            }

            if (!user.isCredentialsNonExpired()) {
                throw new CredentialsExpiredException(resolver.get("authentication.credentials-expired"));
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
