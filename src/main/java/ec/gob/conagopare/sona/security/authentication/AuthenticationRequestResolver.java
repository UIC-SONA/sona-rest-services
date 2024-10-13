package ec.gob.conagopare.sona.security.authentication;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

import java.util.Optional;

@FunctionalInterface
public interface AuthenticationRequestResolver<T extends Authentication, V extends Exception> {

    Optional<T> resolve(HttpServletRequest request) throws V;
}
