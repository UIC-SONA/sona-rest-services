package ec.gob.conagopare.sona.modules.appointments.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import ec.gob.conagopare.sona.modules.appointments.dto.AppointmentRange;
import ec.gob.conagopare.sona.modules.user.models.User;
import io.github.luidmidev.springframework.data.crud.jpa.utils.JpaEnumCandidate;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "appointment")
public class Appointment implements Persistable<Long> {

    public static final String ATTENDANT_ATTRIBUTE = "attendant";
    public static final String PROFESSIONAL_ATTRIBUTE = "professional";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Integer hour;

    @Column(nullable = false)
    private boolean canceled;

    @Column(length = 1000)
    private String cancellationReason;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private Type type;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "attendant_id", nullable = false)
    private User attendant;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "professional_id", nullable = false)
    private User professional;

    @JsonProperty("range")
    private AppointmentRange getRange() {
        return new AppointmentRange(date, hour, hour + 1);
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    @RequiredArgsConstructor
    public enum Type implements JpaEnumCandidate {
        VIRTUAL("virtual"),
        PRESENTIAL("presencial");

        private final String spanishName;

        @Override
        public boolean isCandidate(String value) {
            return spanishName.contains(value.toLowerCase());
        }
    }
}
