package ec.gob.conagopare.sona.models;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "authority")
public class Authority implements GrantedAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    @Builder.Default
    @Column(nullable = false)
    private boolean professional = false;

    @JsonIgnore
    private String description;

    @JsonIgnore
    @Override
    public String getAuthority() {
        return name;
    }

    public boolean is(String name) {
        return this.name.equals(name);
    }

    public boolean in(String... names) {
        for (String n : names) {
            if (this.name.equals(n)) return true;
        }
        return false;
    }


}
