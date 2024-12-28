package ec.gob.conagopare.sona.modules.content.models;


import ec.gob.conagopare.sona.application.configuration.auditor.Auditable;
import io.github.luidmidev.storage.PurgableStored;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "didactic_content")
public class DidacticContent extends Auditable implements Persistable<UUID>, PurgableStored {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String image;

    @Override
    public boolean isNew() {
        return id == null;
    }

    @Override
    public String[] filesFullPaths() {
        return new String[]{image};
    }
}
