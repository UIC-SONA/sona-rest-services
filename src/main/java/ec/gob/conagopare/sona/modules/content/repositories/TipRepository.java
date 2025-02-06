package ec.gob.conagopare.sona.modules.content.repositories;


import ec.gob.conagopare.sona.modules.content.models.Tip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public interface TipRepository extends JpaRepository<Tip, UUID> {

    @Query("SELECT new map(" +
            "t.id as id, " +
            "t.title as title, " +
            "t.summary as summary, " +
            "t.description as description, " +
            "t.tags as tags, " +
            "t.image as image, " +
            "t.active as active, " +
            "t.createdBy as createdBy, " +
            "t.createdDate as createdDate, " +
            "t.lastModifiedBy as lastModifiedBy, " +
            "t.lastModifiedDate as lastModifiedDate, " +
            "COALESCE((SELECT tr.value FROM TipRate tr WHERE tr.tip = t AND tr.user.id = :userId), null) as myRate, " +
            "COALESCE(AVG(tr.value), 0) as averageRate, " +
            "COUNT(tr) as totalRate" +
            ") " +
            "FROM Tip t " +
            "LEFT JOIN TipRate tr ON tr.tip = t " +
            "WHERE t.id = :tipId " +
            "GROUP BY t.id")
    Optional<Map<String, Object>> findByIdMapWithRates(
            @Param("tipId") UUID id,
            @Param("userId") Long userId
    );

    @Query("SELECT new map(" +
            "t.id as id, " +
            "t.title as title, " +
            "t.summary as summary, " +
            "t.description as description, " +
            "t.tags as tags, " +
            "t.image as image, " +
            "t.active as active, " +
            "t.createdBy as createdBy, " +
            "t.createdDate as createdDate, " +
            "t.lastModifiedBy as lastModifiedBy, " +
            "t.lastModifiedDate as lastModifiedDate, " +
            "COALESCE((SELECT tr.value FROM TipRate tr WHERE tr.tip = t AND tr.user.id = :userId), null) as myRate, " +
            "COALESCE(AVG(tr.value), 0) as averageRate, " +
            "COUNT(tr) as totalRate" +
            ") " +
            "FROM Tip t " +
            "LEFT JOIN TipRate tr ON tr.tip = t " +
            "WHERE (:active IS NULL OR t.active = :active) " +
            "GROUP BY t.id")
    Page<Map<String, Object>> findAllMapWithRatings(
            @Param("userId") Long userId,
            @Param("active") Boolean active,
            Pageable pageable
    );

    @Query("SELECT new map(" +
            "t.id as id, " +
            "t.title as title, " +
            "t.summary as summary, " +
            "t.description as description, " +
            "t.tags as tags, " +
            "t.image as image, " +
            "t.active as active, " +
            "t.createdBy as createdBy, " +
            "t.createdDate as createdDate, " +
            "t.lastModifiedBy as lastModifiedBy, " +
            "t.lastModifiedDate as lastModifiedDate, " +
            "COALESCE((SELECT tr.value FROM TipRate tr WHERE tr.tip = t AND tr.user.id = :userId), null) as myRate, " +
            "COALESCE(AVG(tr.value), 0) as averageRate, " +
            "COUNT(tr) as totalRate" +
            ") " +
            "FROM Tip t " +
            "LEFT JOIN TipRate tr ON tr.tip = t " +
            "LEFT JOIN t.tags tag " +
            "WHERE (:active IS NULL OR t.active = :active) " +
            "AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "    OR LOWER(t.summary) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "    OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "    OR LOWER(tag) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "GROUP BY t.id")
    Page<Map<String, Object>> searchAllMapWithRatings(
            @Param("search") String search,
            @Param("userId") Long userId,
            @Param("active") Boolean active,
            Pageable pageable
    );

    @Query("SELECT new map(" +
            "t.id as id, " +
            "t.title as title, " +
            "t.summary as summary, " +
            "t.description as description, " +
            "t.tags as tags, " +
            "t.image as image, " +
            "t.active as active, " +
            "t.createdBy as createdBy, " +
            "t.createdDate as createdDate, " +
            "t.lastModifiedBy as lastModifiedBy, " +
            "t.lastModifiedDate as lastModifiedDate, " +
            "COALESCE((SELECT tr.value FROM TipRate tr WHERE tr.tip = t AND tr.user.id = :userId), null) as myRate, " +
            "COALESCE(AVG(tr.value), 0) as averageRate, " +
            "COUNT(tr) as totalRate" +
            ") " +
            "FROM Tip t " +
            "LEFT JOIN TipRate tr ON tr.tip = t " +
            "GROUP BY t.id " +
            "ORDER BY AVG(tr.value) DESC " +
            "LIMIT :limit")
    List<Map<String, Object>> findTopRating(int limit);

    default Optional<Tip> findByIdWithRates(UUID id, Long userId) {
        var map = findByIdMapWithRates(id, userId);
        return map.map(toTip());
    }

    default Page<Tip> findAllWithRates(Long userId, Boolean active, Pageable pageable) {
        var page = findAllMapWithRatings(userId, active, pageable);
        return page.map(toTip());
    }

    default Page<Tip> searchAllWithRates(String search, Long userId, Boolean active, Pageable pageable) {
        var page = searchAllMapWithRatings(search, userId, active, pageable);
        return page.map(toTip());
    }

    default List<Tip> topRating(int limit) {
        var list = findTopRating(limit);
        return list.stream().map(toTip()).toList();
    }

    private static Function<Map<String, Object>, Tip> toTip() {
        return m -> {
            var tip = new Tip();
            tip.setId((UUID) m.get("id"));
            tip.setTitle((String) m.get("title"));
            tip.setSummary((String) m.get("summary"));
            tip.setDescription((String) m.get("description"));
            tip.setTags((List<String>) m.get("tags"));
            tip.setImage((String) m.get("image"));
            tip.setActive((Boolean) m.get("active"));
            tip.setCreatedBy((String) m.get("createdBy"));
            tip.setCreatedDate((LocalDateTime) m.get("createdDate"));
            tip.setLastModifiedBy((String) m.get("lastModifiedBy"));
            tip.setLastModifiedDate((LocalDateTime) m.get("lastModifiedDate"));
            tip.setMyRate((Integer) m.get("myRate"));
            tip.setAverageRate((Double) m.get("averageRate"));
            tip.setTotalRate((Long) m.get("totalRate"));
            return tip;
        };
    }

    @Query("SELECT image FROM Tip WHERE id = :id")
    Optional<String> getImagePathById(UUID id);

}
