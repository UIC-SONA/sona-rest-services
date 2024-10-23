package ec.gob.conagopare.sona.security.authentication;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

@FunctionalInterface
public interface AuthenticationRequestProvider<T extends Authentication> {

    AuthenticationResult<T> resolve(HttpServletRequest request);

    record AuthenticationResult<T extends Authentication>(T authentication) {

        public static <T extends Authentication> AuthenticationResult<T> authenticated(T authentication) {
            authentication.setAuthenticated(true);
            return new AuthenticationResult<>(authentication);
        }

        public static <T extends Authentication> AuthenticationResult<T> unauthenticated() {
            return new AuthenticationResult<>(null);
        }

        public boolean success() {
            return authentication != null && authentication.isAuthenticated();
        }
    }
}
