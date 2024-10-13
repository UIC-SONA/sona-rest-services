package ec.gob.conagopare.sona.utils;

import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;


@Log4j2
public class FilterQueryBuilder {

    public enum Order {
        ASC, DESC
    }

    private final ParammeterBuilder parammeterBuilder = new ParammeterBuilder();

    private final String select;
    private final String from;
    private final List<String> innerJoins = new ArrayList<>();
    private final List<String> conditions = new ArrayList<>();
    private final List<String> groupBy = new ArrayList<>();
    private final List<String> orderBy = new ArrayList<>();
    private Order order = Order.ASC;
    private Integer limit = null;

    public FilterQueryBuilder(String select, String from) {
        this.select = select;
        this.from = from;
    }

    public static FilterQueryBuilder select(String select, String from) {
        return new FilterQueryBuilder(select, from);
    }

    /**
     * @param paramName Nombre del parametro que se va a agregar al filtro
     * @param value     Valor del parametro en la consulta
     * @param condition Condicion que se va a agregar al filtro
     * @param inners    Tablas que se van a unir para llegar al parametro que se va a agregar al filtro
     * @return this
     */
    public FilterQueryBuilder addFilter(String paramName, Object value, String condition, String... inners) {
        if (value == null) return this;
        parammeterBuilder.addValue(paramName, value);
        conditions.add(condition);
        return addInners(inners);
    }

    /**
     * @param inners Tablas que se van a unir a la consulta
     * @return this
     */
    public FilterQueryBuilder addInners(String... inners) {
        for (var inner : inners) {
            if (!innerJoins.contains(inner)) innerJoins.add(inner);
        }
        return this;
    }

    public FilterQueryBuilder addGroupBy(String groupBy) {
        this.groupBy.add(groupBy);
        return this;
    }

    public FilterQueryBuilder addGroupBy(String... groupBy) {
        Collections.addAll(this.groupBy, groupBy);
        return this;
    }

    public FilterQueryBuilder withOrderBy(String... orderBy) {
        return withOrderBy(Order.ASC, orderBy);
    }

    public FilterQueryBuilder withOrderBy(Order order, String... orderBy) {
        this.orderBy.clear();
        this.orderBy.addAll(Arrays.asList(orderBy));
        this.order = order;
        return this;
    }

    public FilterQueryBuilder withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public FilterQuery build() {
        var query = new StringBuilder();

        query.append(" \n");
        query.append("SELECT ");
        query.append(select);
        query.append(" FROM ");
        query.append(from);
        query.append(" \n");

        for (var innerJoin : innerJoins) {
            query.append("INNER JOIN ");
            query.append(innerJoin);
            query.append(" \n");
        }

        clauseGenerator(conditions, query, " WHERE ", " AND ");
        clauseGenerator(groupBy, query, " GROUP BY ", ", ");
        clauseGenerator(orderBy, query, " ORDER BY ", ", ", sb -> {
            sb.append(" ");
            sb.append(order);
        });

        if (limit != null) {
            query.append(" LIMIT ");
            query.append(limit);
        }

        return new FilterQuery(query.toString(), parammeterBuilder.build());
    }

    private void clauseGenerator(List<String> conditions, StringBuilder query, String clause, String separator) {
        clauseGenerator(conditions, query, clause, separator, sb -> {
        });
    }

    private void clauseGenerator(List<String> conditions, StringBuilder query, String clause, String separator, Consumer<StringBuilder> consumer) {
        if (!conditions.isEmpty()) {
            query.append(clause);
            query.append(conditions.get(0));
            for (int i = 1; i < conditions.size(); i++) {
                query.append(separator);
                query.append(conditions.get(i));
            }
            consumer.accept(query);
            query.append(" \n");
        }
    }

    public record FilterQuery(String query, SqlParameterSource params) {
    }

}
