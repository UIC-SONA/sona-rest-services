package ec.gob.conagopare.sona.utils;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

public class ParammeterBuilder {

    private final MapSqlParameterSource params = new MapSqlParameterSource();

    public void addValue(String key, Object value) {
        params.addValue(key, value);
    }

    public SqlParameterSource build() {
        if (params.getValues().isEmpty()) {
            throw new IllegalStateException("No parameters added");
        }
        return params;
    }
}
