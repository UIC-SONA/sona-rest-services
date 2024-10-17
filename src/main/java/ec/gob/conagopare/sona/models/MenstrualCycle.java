package ec.gob.conagopare.sona.models;

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

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


}
