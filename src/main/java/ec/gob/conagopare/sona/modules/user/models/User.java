package ec.gob.conagopare.sona.modules.user.models;


import ec.gob.conagopare.sona.application.configuration.auditor.Auditable;
import io.github.luidmidev.storage.PurgableStored;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "sona_user")
public class User extends Auditable implements Persistable<Long>, PurgableStored {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String keycloakId;

    @Column
    private String profilePicturePath;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean anonymous = false;

    // USER INFORMATION SYNCED FROM KEYCLOAK

    @Column
    protected String username;

    @Column
    protected String firstName;

    @Column
    protected String lastName;

    @Column
    protected String email;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER, targetClass = Authority.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_authorities", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "authority")
    private Set<Authority> authorities = new HashSet<>();

    public boolean is(Authority... authorities) {
        return this.authorities.containsAll(Set.of(authorities));
    }

    public boolean isAny(Authority... authorities) {
        return Set.of(authorities).stream().anyMatch(this.authorities::contains);
    }

    @Override
    public String[] filesFullPaths() {
        return new String[]{profilePicturePath};
    }

    @Override
    public boolean isNew() {
        return id == null;
    }
}
