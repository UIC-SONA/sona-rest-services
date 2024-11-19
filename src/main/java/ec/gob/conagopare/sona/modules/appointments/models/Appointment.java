package ec.gob.conagopare.sona.modules.appointments.models;

import ec.gob.conagopare.sona.modules.user.models.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "appointment")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(nullable = false)
    private boolean canceled;

    @Builder.Default
    @Column(nullable = false)
    private boolean attended = false;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private Type type;

    @ManyToOne
    @JoinColumn(name = "attendant_id", nullable = false)
    private User attendant;

    @ManyToOne
    @JoinColumn(name = "professional_id", nullable = false)
    private User professional;

    enum Type {
        VIRTUAL,
        PRESENTIAL
    }

}
