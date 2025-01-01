package ec.gob.conagopare.sona.modules.user.models;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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
        return Arrays.stream(values()).filter(authority -> authority.getAuthority().equals(name)).findFirst();
    }

    public static Collection<Authority> from(String... names) {
        return map(Arrays.stream(names));
    }


    public static Collection<Authority> from(Collection<String> names) {
        return map(names.stream());
    }

    private static Collection<Authority> map(Stream<String> stream) {
        return stream
                .map(Authority::from)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    public static String[] asString(Collection<Authority> authorities) {
        return authorities.stream().map(Authority::getAuthority).toArray(String[]::new);
    }

    public static String[] asString(Authority... records) {
        return asString(Set.of(records));
    }
}

