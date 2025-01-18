package ec.gob.conagopare.sona.application.firebase.messaging;


import ec.gob.conagopare.sona.modules.user.models.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "device_token")
public class DeviceToken {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String token;


    @Column
    private LocalDateTime refreshedAt;

}
