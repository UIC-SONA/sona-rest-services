package ec.gob.conagopare.sona.models;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "token_forget_password")
public class TokenForgetPassword implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;

    @Email(message = "El email no es válido")
    private String email;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() {
        var now = LocalDateTime.now();
        return now.isAfter(this.createdAt.plusMinutes(30));
    }


    @PrePersist
    public void onBeforeConvert() {
        setCreatedAt(LocalDateTime.now());
    }
}



