package ec.gob.conagopare.sona.modules.user.entities;


import ec.gob.conagopare.sona.application.configuration.auditor.Auditable;
import io.github.luidmidev.storage.PurgableStored;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "sona_user")
public class User extends Auditable implements PurgableStored {

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
