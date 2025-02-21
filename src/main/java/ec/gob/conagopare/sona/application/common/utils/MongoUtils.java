package ec.gob.conagopare.sona.application.common.utils;

import ec.gob.conagopare.sona.application.common.schemas.CountResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.ArrayList;
import java.util.List;

public final class MongoUtils {
    private MongoUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static <T> Page<T> getPage(MongoOperations mongo, Pageable pageable, List<AggregationOperation> operations, String collectionName, Class<T> targetClass) {
        if (pageable.getSort().isSorted()) {
            operations.add(Aggregation.sort(pageable.getSort()));
        }

        if (pageable.isUnpaged()) {
            // Ejecutar agregación sin paginación
            var aggregation = Aggregation.newAggregation(operations);
            var results = mongo.aggregate(aggregation, collectionName, targetClass);
            return new PageImpl<>(results.getMappedResults());
        }

        // Agregar paginación
        operations.add(Aggregation.skip((long) pageable.getPageNumber() * pageable.getPageSize()));
        operations.add(Aggregation.limit(pageable.getPageSize()));

        // Ejecutar agregación paginada
        var aggregation = Aggregation.newAggregation(operations);
        var results = mongo.aggregate(aggregation, collectionName, targetClass);

        // Crear agregación de conteo optimizada, eliminando la etapa de paginación
        var countOperations = new ArrayList<>(operations.subList(0, operations.size() - 2));
        countOperations.add(Aggregation.count().as("total"));
        var countAggregation = Aggregation.newAggregation(countOperations);
        AggregationResults<CountResult> countResult = mongo.aggregate(countAggregation, collectionName, CountResult.class);
        var count = countResult.getUniqueMappedResult();

        return PageableExecutionUtils.getPage(
                results.getMappedResults(),
                pageable,
                () -> count != null ? count.getTotal() : 0
        );
    }

    public static <T> Page<T> getPage(MongoOperations mongo, Pageable pageable, List<AggregationOperation> operations, Class<T> targetClass) {
        var collectionName = mongo.getCollectionName(targetClass);
        return getPage(mongo, pageable, operations, collectionName, targetClass);
    }
}