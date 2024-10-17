package ec.gob.conagopare.sona.security.authentication;


import ec.gob.conagopare.sona.security.UserDetailsAuthenticaction;
import ec.gob.conagopare.sona.security.jwt.Jwt;
import ec.gob.conagopare.sona.services.UserService;
import ec.gob.conagopare.sona.utils.HttpServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationRequestProvider implements AuthenticationRequestProvider<UserDetailsAuthenticaction> {

    private final Jwt jwt;
    private final UserService userService;
    private final UserDetailsChecker userDetailsChecker;

    @Override
    public AuthenticationResult<UserDetailsAuthenticaction> resolve(@NotNull HttpServletRequest request) {

        final String token = HttpServletUtils.extractBearerToken(request);

        if (token == null) {
            return AuthenticationResult.unauthenticated();
        }

        var issuer = Jwt.decode(token).getIssuer();

        if (!Objects.equals(issuer, jwt.getIssuer())) {
            return AuthenticationResult.unauthenticated();
        }

        var id = jwt.getId(token);
        var user = userService.find(UUID.fromString(id));

        userDetailsChecker.check(user);

        return AuthenticationResult.of(new UserDetailsAuthenticaction(user));
    }
}
