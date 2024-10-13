package ec.gob.conagopare.sona.security.authentication;


import ec.gob.conagopare.sona.security.UserDetailsAuthenticaction;
import ec.gob.conagopare.sona.security.jwt.JwtService;
import ec.gob.conagopare.sona.services.UserService;
import ec.gob.conagopare.sona.utils.HttpServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationRequestResolver implements AuthenticationRequestResolver<UserDetailsAuthenticaction, RuntimeException> {

    private final JwtService jwtService;
    private final UserService userService;
    private final UserDetailsChecker userDetailsChecker;

    @Override
    public Optional<UserDetailsAuthenticaction> resolve(@NotNull HttpServletRequest request) {

        final String token = HttpServletUtils.extractBearerToken(request);
        if (token == null) {
            return Optional.empty();
        }

        var issuer = JwtService.decode(token).getIssuer();

        if (!Objects.equals(issuer, jwtService.getIssuer())) {
            return Optional.empty();
        }

        var id = jwtService.getId(token);
        var user = userService.findById(Long.parseLong(id));
        userDetailsChecker.check(user);
        return Optional.of(new UserDetailsAuthenticaction(user));
    }
}
