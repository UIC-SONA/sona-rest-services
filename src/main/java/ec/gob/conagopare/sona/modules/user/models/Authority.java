package ec.gob.conagopare.sona.modules.user.models;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

import java.util.Arrays;
import java.util.Optional;

@RequiredArgsConstructor
public enum Authority implements GrantedAuthority {
    ADMIN("ROLE_admin"),
    ADMINISTRATIVE("ROLE_administrative"),
    USER("ROLE_user");


    private final String value;

    @Override
    public String getAuthority() {
        return value;
    }

    public static Optional<Authority> from(String name) {
        return Arrays.stream(Authority.values()).filter(authority -> authority.getAuthority().equals(name)).findFirst();
    }
}
