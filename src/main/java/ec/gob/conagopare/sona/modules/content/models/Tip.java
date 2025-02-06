package ec.gob.conagopare.sona.modules.content.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ec.gob.conagopare.sona.application.configuration.auditor.Auditable;
import io.github.luidmidev.storage.PurgableStored;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tip")
public class Tip extends Auditable implements Persistable<UUID>, PurgableStored {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "tip_tags",
            joinColumns = @JoinColumn(name = "tip_id", nullable = false)
    )
    @Column(name = "tag", nullable = false)
    private List<String> tags = new ArrayList<>();

    @JsonIgnore
    @Column(nullable = false)
    private String image;

    @Column(nullable = false)
    private boolean active;

    @Transient
    private Integer myRate;

    @Transient
    private Double averageRate;

    @Transient
    private Long totalRate;

    public Tip(UUID id,
               String createdBy,
               LocalDateTime createdDate,
               String lastModifiedBy,
               LocalDateTime lastModifiedDate,
               String title,
               String summary,
               String description,
               List<String> tags,
               String image,
               boolean active,
               Integer myRate,
               Double averageRate,
               Long totalRate
    ) {
        super(createdBy, createdDate, lastModifiedBy, lastModifiedDate);
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.tags = tags;
        this.image = image;
        this.active = active;
        this.myRate = myRate;
        this.averageRate = averageRate;
        this.totalRate = totalRate;
    }

    @Override
    public String[] filesFullPaths() {
        return new String[]{image};
    }

    @Override
    public boolean isNew() {
        return id == null;
    }
}
