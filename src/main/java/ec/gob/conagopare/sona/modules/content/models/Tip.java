package ec.gob.conagopare.sona.modules.content.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ec.gob.conagopare.sona.application.common.converters.JsonConverter;
import ec.gob.conagopare.sona.application.configuration.auditor.Auditable;
import io.github.luidmidev.springframework.data.crud.core.CRUDModel;
import io.github.luidmidev.storage.PurgableStored;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tip")
public class Tip extends Auditable implements PurgableStored, CRUDModel<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Convert(converter = JsonConverter.ListStringConverter.class)
    private List<String> tags;

    @JsonIgnore
    @Column(nullable = false)
    private String image;

    @Column(nullable = false)
    private boolean active;

    @Override
    public String[] filesFullPaths() {
        return new String[]{image};
    }
}