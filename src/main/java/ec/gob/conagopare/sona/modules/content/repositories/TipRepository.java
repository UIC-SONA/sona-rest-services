package ec.gob.conagopare.sona.modules.content.repositories;


import ec.gob.conagopare.sona.modules.content.models.Tip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public interface TipRepository extends JpaRepository<Tip, UUID> {

    @Query("SELECT new map(" +
            "t as tip, " +
            "COALESCE((SELECT tr.value FROM TipRate tr WHERE tr.tip = t AND tr.user.id = :userId), null) as myRate, " +
            "COALESCE(AVG(tr.value), 0) as averageRate, " +
            "COUNT(tr) as totalRate) " +
            "FROM Tip t " +
            "LEFT JOIN TipRate tr ON tr.tip = t " +
            "WHERE t.id = :tipId " +
            "GROUP BY t")
    Optional<Map<String, Object>> findByIdMapWithRates(
            @Param("tipId") UUID id,
            @Param("userId") Long userId
    );

    @Query("SELECT new map(" +
            "t as tip, " +
            "COALESCE((SELECT tr.value FROM TipRate tr WHERE tr.tip = t AND tr.user.id = :userId), null) as myRate, " +
            "COALESCE(AVG(tr.value), 0) as averageRate, " +
            "COUNT(tr) as totalRate) " +
            "FROM Tip t " +
            "LEFT JOIN TipRate tr ON tr.tip = t " +
            "WHERE (:active IS NULL OR t.active = :active) " +
            "GROUP BY t")
    Page<Map<String, Object>> findAllMapWithRatings(
            @Param("userId") Long userId,
            @Param("active") Boolean active,
            Pageable pageable
    );

    @Query("SELECT new map(" +
            "t as tip, " +
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
    Page<Map<String, Object>> searchAllMapWithRatings(
            @Param("search") String search,
            @Param("userId") Long userId,
            @Param("active") Boolean active,
            Pageable pageable
    );

    @Query("SELECT new map(" +
            "t as tip, " +
            "COALESCE(AVG(tr.value), 0) as averageRate, " +
            "COUNT(tr) as totalRate) " +
            "FROM Tip t " +
            "LEFT JOIN TipRate tr ON tr.tip = t " +
            "GROUP BY t.id " +
            "ORDER BY AVG(tr.value) DESC " +
            "LIMIT :limit")
    List<Map<String, Object>> findTopRating(int limit);

    default Optional<Tip> findByIdWithRates(UUID id, Long userId) {
        var map = findByIdMapWithRates(id, userId);
        if (map.isEmpty()) return Optional.empty();
        if (map.get().isEmpty()) return Optional.empty();
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
        return list.stream()
                .map(toTip())
                .sorted((a, b) -> Double.compare(b.getAverageRate(), a.getAverageRate()))
                .toList();
    }

    private static Function<Map<String, Object>, Tip> toTip() {
        return m -> {
            var tip = (Tip) m.get("tip");
            tip.setMyRate((Integer) m.get("myRate"));
            tip.setAverageRate((Double) m.get("averageRate"));
            tip.setTotalRate((Long) m.get("totalRate"));
            return tip;
        };
    }

    @Query("SELECT image FROM Tip WHERE id = :id")
    Optional<String> getImagePathById(UUID id);

}
