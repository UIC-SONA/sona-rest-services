package ec.gob.conagopare.sona.modules.user.entities;


import io.github.luidmidev.storage.PurgableStored;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "sona_user")
public class User implements PurgableStored {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String keycloakId;

    @Column(unique = true)
    private String ci;

    @Column(nullable = false)
    protected LocalDate dateOfBirth;

    @Column
    private String profilePicturePath;

    @Override
    public String[] filesFullPaths() {
        return new String[]{profilePicturePath};
    }
}
