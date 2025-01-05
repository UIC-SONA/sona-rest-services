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
    LEGAL_PROFESSIONAL("ROLE_legal_professional"),
    MEDICAL_PROFESSIONAL("ROLE_medical_professional"),
    USER("ROLE_user");

    private final String roleName;

    @Override
    public String getAuthority() {
        return roleName;
    }

    public static Optional<Authority> findByRole(String roleName) {
        return Arrays.stream(values())
                .filter(authority -> authority.getAuthority().equals(roleName))
                .findFirst();
    }

    public static Collection<Authority> parseAuthorities(String... roleNames) {
        return parseAuthorities(Arrays.stream(roleNames));
    }


    public static Collection<Authority> parseAuthorities(Collection<String> roleNames) {
        return parseAuthorities(roleNames.stream());
    }

    public static Collection<Authority> parseAuthorities(Stream<String> roleStream) {
        return roleStream
                .map(Authority::findByRole)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    public static Collection<Authority> valuesOf(String... roleNames) {
        return valuesOf(Arrays.asList(roleNames));
    }

    public static Collection<Authority> valuesOf(Collection<String> roleNames) {
        return valuesOf(roleNames.stream());
    }

    public static Collection<Authority> valuesOf(Stream<String> roleStream) {
        return roleStream
                .map(Authority::valueOf)
                .collect(Collectors.toSet());
    }

    public static String[] convertToRoleNames(Collection<Authority> authorities) {
        return authorities.stream()
                .map(Authority::getAuthority
                ).toArray(String[]::new);
    }

    public static String[] convertToRoleNames(Authority... authorities) {
        return convertToRoleNames(Set.of(authorities));
    }
}

