package ec.gob.conagopare.sona.modules.user.models;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

import java.util.Arrays;

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

    public static boolean exists(String name) {
        return Arrays.stream(Authority.values()).anyMatch(authority -> authority.getAuthority().equals(name));
    }
}
