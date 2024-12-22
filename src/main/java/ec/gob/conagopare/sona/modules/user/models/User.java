package ec.gob.conagopare.sona.modules.user.models;


import ec.gob.conagopare.sona.application.configuration.auditor.Auditable;
import io.github.luidmidev.storage.PurgableStored;
import jakarta.persistence.*;
import lombok.*;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.Persistable;

import java.util.Collection;

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

    @Transient
    private UserRepresentation representation;

    @Transient
    private Collection<Authority> authorities;

    @Override
    public String[] filesFullPaths() {
        return new String[]{profilePicturePath};
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

//    private ProfessionalSchedule professionalSchedule;
//
//    @Data
//    public static class ProfessionalSchedule {
//        private LocalDate scheduleUpTo;
//        private boolean scheduleEnabled;
//        private List<ProfessionalScheduleDay> scheduleDays = List.of();
//    }
//
//    @Data
//    public static class ProfessionalScheduleDay {
//        private DayOfWeek day;
//        private Integer startHour;
//        private Integer endHour;
//    }
}
