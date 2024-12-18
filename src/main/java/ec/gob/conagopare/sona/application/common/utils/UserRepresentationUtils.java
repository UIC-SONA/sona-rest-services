package ec.gob.conagopare.sona.application.common.utils;

import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.Sort;

import java.util.*;
import java.util.function.Predicate;

public class UserRepresentationUtils {

    private UserRepresentationUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static Predicate<UserRepresentation> filterPredicate(String search) {
        return representation -> {
            var searchLowerCase = search.toLowerCase();
            var email = representation.getEmail().toLowerCase();
            var username = representation.getUsername().toLowerCase();
            var firstName = representation.getFirstName().toLowerCase();
            var lastName = representation.getLastName().toLowerCase();
            return email.contains(searchLowerCase) || username.contains(searchLowerCase) || firstName.contains(searchLowerCase) || lastName.contains(searchLowerCase);
        };
    }

    public static List<Comparator<UserRepresentation>> getComparators(Sort sort) {
        if (sort.isUnsorted()) {
            return List.of();
        }
        var comparators = new ArrayList<Comparator<UserRepresentation>>();
        for (var order : sort) {
            var comparator = switch (order.getProperty()) {
                case "email" -> Comparator.comparing(UserRepresentation::getEmail);
                case "username" -> Comparator.comparing(UserRepresentation::getUsername);
                case "firstName" -> Comparator.comparing(UserRepresentation::getFirstName);
                case "lastName" -> Comparator.comparing(UserRepresentation::getLastName);
                default -> null;
            };
            if (comparator != null) {
                comparators.add(order.isAscending() ? comparator : comparator.reversed());
            }
        }

        return comparators;
    }

    public static Collection<UserRepresentation> sort(Collection<UserRepresentation> representations, Sort sort) {
        if (sort.isUnsorted()) {
            return Collections.unmodifiableCollection(representations);
        }
        var comparators = getComparators(sort);
        return representations.stream().sorted((a, b) -> {
            for (var comparator : comparators) {
                var result = comparator.compare(a, b);
                if (result != 0) return result;
            }
            return 0;
        }).toList();
    }
}
