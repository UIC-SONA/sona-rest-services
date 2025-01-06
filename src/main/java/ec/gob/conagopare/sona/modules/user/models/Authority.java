package ec.gob.conagopare.sona.modules.user.models;

import io.github.luidmidev.springframework.data.crud.jpa.utils.EnumSearchable;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@RequiredArgsConstructor
public enum Authority implements GrantedAuthority, EnumSearchable {
    ADMIN("ROLE_admin", "administrador"),
    ADMINISTRATIVE("ROLE_administrative", "administrativo"),
    LEGAL_PROFESSIONAL("ROLE_legal_professional", "profesional legal"),
    MEDICAL_PROFESSIONAL("ROLE_medical_professional", "profesional médico"),
    USER("ROLE_user", "usuario");

    private final String roleName;
    private final String spanishName;

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
                .map(Authority::getAuthority)
                .toArray(String[]::new);
    }

    public static String[] convertToRoleNames(Authority... authorities) {
        return convertToRoleNames(Set.of(authorities));
    }

    @Override
    public boolean matches(String value) {
        return spanishName.contains(value.toLowerCase());
    }
}

