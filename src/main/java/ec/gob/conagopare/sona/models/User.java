package ec.gob.conagopare.sona.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ketoru.store.core.PurgableFileStore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "sona_user")
public class User implements UserDetails, PurgableFileStore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String lastname;

    @Column(nullable = false, length = 100, unique = true)
    private String username;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column
    private String profilePicture;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean accountNonLocked = true;


    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_authorities",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "authority_id")
    )
    private List<Authority> authorities;

    @Override
    public Collection<Authority> getAuthorities() {
        return Collections.unmodifiableList(authorities);
    }


    public boolean hasAuthority(String... authorities) {
        return this.authorities.stream()
                .map(Authority::getName)
                .anyMatch(List.of(authorities)::contains);
    }

    @Override
    public String[] filesFullPaths() {
        return new String[]{profilePicture};
    }
}
