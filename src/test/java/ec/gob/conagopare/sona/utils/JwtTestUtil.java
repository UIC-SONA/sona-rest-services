package ec.gob.conagopare.sona.utils;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.*;

public class JwtTestUtil {

    public static SecurityContext createJwtContext(String subject, List<String> roles) {

        var claims = new HashMap<String, Object>();
        claims.put("sub", subject);
        claims.put("roles", roles);

        var jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(subject)
                .claim("scope", String.join(" ", roles))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(claims1 -> claims1.putAll(claims))
                .build();

        var authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();

        var authentication = new JwtAuthenticationToken(jwt, authorities);

        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }


    public static Jwt getJwt() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken();
        }
        throw new IllegalStateException("No JWT found in SecurityContext");
    }


}