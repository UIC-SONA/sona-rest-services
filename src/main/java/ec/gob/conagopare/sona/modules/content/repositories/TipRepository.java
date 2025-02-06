package ec.gob.conagopare.sona.modules.content.repositories;


import ec.gob.conagopare.sona.modules.content.models.Tip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TipRepository extends JpaRepository<Tip, UUID> {

    @Query("SELECT new ec.gob.conagopare.sona.modules.content.models.Tip(" +
            "t.id, t.title, t.summary, t.description, t.tags, t.image, t.active, " +
            "COALESCE((SELECT tr.value FROM TipRate tr WHERE tr.tip = t AND tr.user.id = :userId), null) as myRate, " +
            "COALESCE(AVG(tr.value), 0) as averageRate, " +
            "COUNT(tr) as totalRate) " +
            "FROM Tip t " +
            "LEFT JOIN TipRate tr ON tr.tip = t " +
            "WHERE t.id = :tipId " +
            "GROUP BY t")
    Optional<Tip> findByIdWithRates(
            @Param("tipId") UUID id,
            @Param("userId") Long userId
    );

    @Query("SELECT new ec.gob.conagopare.sona.modules.content.models.Tip(" +
            "t.id, t.title, t.summary, t.description, t.tags, t.image, t.active, " +
            "COALESCE((SELECT tr.value FROM TipRate tr WHERE tr.tip = t AND tr.user.id = :userId), null) as myRate, " +
            "COALESCE(AVG(tr.value), 0) as averageRate, " +
            "COUNT(tr) as totalRate) " +
            "FROM Tip t " +
            "LEFT JOIN TipRate tr ON tr.tip = t " +
            "WHERE (:active IS NULL OR t.active = :active) " +
            "GROUP BY t")
    Page<Tip> findAllWithRates(
            @Param("userId") Long userId,
            @Param("active") Boolean active,
            Pageable pageable
    );

    @Query("SELECT new ec.gob.conagopare.sona.modules.content.models.Tip(" +
            "t.id, t.title, t.summary, t.description, t.tags, t.image, t.active, " +
            "COALESCE((SELECT tr.value FROM TipRate tr WHERE tr.tip = t AND tr.user.id = :userId), null) as myRate, " +
            "COALESCE(AVG(tr.value), 0) as averageRate, " +
            "COUNT(tr) as totalRate) " +
            "FROM Tip t " +
            "LEFT JOIN TipRate tr ON tr.tip = t " +
            "LEFT JOIN t.tags tag " +
            "WHERE (:active IS NULL OR t.active = :active) " +
            "AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "    OR LOWER(t.summary) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "    OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "    OR LOWER(tag) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "GROUP BY t")
    Page<Tip> searchAllWithRates(
            @Param("search") String search,
            @Param("userId") Long userId,
            @Param("active") Boolean active,
            Pageable pageable
    );

    @Query("SELECT new ec.gob.conagopare.sona.modules.content.models.Tip(" +
            "t.id, t.title, t.summary, t.description, t.tags, t.image, t.active, " +
            "COALESCE((SELECT tr.value FROM TipRate tr WHERE tr.tip = t AND tr.user.id = :userId), null) as myRate, " +
            "COALESCE(AVG(tr.value), 0) as averageRate, " +
            "COUNT(tr) as totalRate) " +
            "FROM Tip t " +
            "LEFT JOIN TipRate tr ON tr.tip = t " +
            "GROUP BY t.id " +
            "ORDER BY AVG(tr.value) DESC " +
            "LIMIT :limit")
    List<Tip> topRating(@Param("limit") int limit, @Param("userId") Long userId);


    @Query("SELECT image FROM Tip WHERE id = :id")
    Optional<String> getImagePathById(UUID id);

}
