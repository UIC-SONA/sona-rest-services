package ec.gob.conagopare.sona.modules.menstrualcycle.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ec.gob.conagopare.sona.modules.user.models.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "cycle_data")
@ToString(exclude = "user")
public class CycleData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private int periodDuration;

    @Column(nullable = false)
    private int cycleLength;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "cycle_data_period_dates",
            joinColumns = @JoinColumn(name = "cycle_data_id", nullable = false)
    )
    @Column(name = "period_date", nullable = false)
    private List<LocalDate> periodDates = new ArrayList<>();

    @JsonIgnore
    @JoinColumn(name = "user_id", nullable = false)
    @OneToOne
    private User user;

}
