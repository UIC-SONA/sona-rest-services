package ec.gob.conagopare.sona.modules.user.models;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;


@RequiredArgsConstructor
public enum Authority implements GrantedAuthority {
    ADMIN("ROLE_admin"),
    ADMINISTRATIVE("ROLE_administrative"),
    PROFESSIONAL("ROLE_professional"),
    LEGAL_PROFESSIONAL("ROLE_legal_professional"),
    MEDICAL_PROFESSIONAL("ROLE_medical_professional"),
    USER("ROLE_user");


    private final String value;

    @Override
    public String getAuthority() {
        return value;
    }

    public static Optional<Authority> from(String name) {
        return Arrays.stream(Authority.values()).filter(authority -> authority.getAuthority().equals(name)).findFirst();
    }

    public static String[] getAuthorities(Collection<Authority> authorities) {
        return authorities.stream().map(Authority::getAuthority).toArray(String[]::new);
    }

    public static String[] getAuthorities(Authority... authorities) {
        return getAuthorities(Set.of(authorities));
    }
}
