package ec.gob.conagopare.sona.modules.menstrualcycle.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ec.gob.conagopare.sona.application.common.converters.JsonConverter;
import ec.gob.conagopare.sona.modules.user.models.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
public class CycleData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private int periodDuration;

    @Column(nullable = false)
    private int cycleLength;

    @Convert(converter = JsonConverter.ListLocalDateConverter.class)
    @Column(nullable = false)
    private List<LocalDate> periodDates = new ArrayList<>();

    @JsonIgnore
    @JoinColumn(name = "user_id", nullable = false)
    @OneToOne
    private User user;

}
