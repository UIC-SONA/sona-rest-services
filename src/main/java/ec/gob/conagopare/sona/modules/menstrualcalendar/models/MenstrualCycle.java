package ec.gob.conagopare.sona.modules.menstrualcalendar.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ec.gob.conagopare.sona.modules.user.entities.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "appointment")
public class MenstrualCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private int periodDuration;

    @Column(nullable = false)
    private int cycleDuration;

    @Column(nullable = false)
    private LocalDate lastPeriodDate;

    @JsonIgnore
    @JoinColumn(name = "user_id", nullable = false)
    @OneToOne
    private User user;

}
