package ec.gob.conagopare.sona.modules.appointments.models;

import ec.gob.conagopare.sona.modules.user.models.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "appointment")
public class Appointment implements Persistable<Long> {

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
    private String cancelationReason;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private Type type;

    @ManyToOne
    @JoinColumn(name = "attendant_id", nullable = false)
    private User attendant;

    @ManyToOne
    @JoinColumn(name = "professional_id", nullable = false)
    private User professional;

    @Override
    public boolean isNew() {
        return id == null;
    }

    public enum Type {
        VIRTUAL,
        PRESENTIAL
    }

}
