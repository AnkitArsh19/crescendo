package com.crescendo.apps.postgresql;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@ActionMapping(appKey = "postgresql", actionKey = "execute-query")
public class PostgreSQLExecuteQueryHandler implements ActionHandler {

    private final ObjectMapper objectMapper;

    public PostgreSQLExecuteQueryHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
public ActionResult execute(ActionContext context) {
        try {
            String sql = value(context.configuration(), "sql", "");
            if (sql.isBlank()) return ActionResult.failure("PostgreSQL SQL is required");

            List<?> params = params(context.configuration().get("params"));
            int maxRows = intValue(context.configuration().get("maxRows"), 100);

            try (Connection connection = DriverManager.getConnection(jdbcUrl(context.credentials()), properties(context.credentials()));
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int i = 0; i < params.size(); i++) {
                    statement.setObject(i + 1, params.get(i));
                }

                boolean hasResultSet = statement.execute();
                if (!hasResultSet) {
                    return ActionResult.success(Map.of("updatedRows", statement.getUpdateCount()));
                }

                List<Map<String, Object>> rows = new ArrayList<>();
                try (ResultSet rs = statement.getResultSet()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    while (rs.next() && rows.size() < Math.max(1, maxRows)) {
                        java.util.LinkedHashMap<String, Object> row = new java.util.LinkedHashMap<>();
                        for (int c = 1; c <= cols; c++) {
                            row.put(meta.getColumnLabel(c), rs.getObject(c));
                        }
                        rows.add(row);
                    }
                }
                return ActionResult.success(Map.of("rows", rows, "count", rows.size()));
            }
        } catch (Exception e) {
            return ActionResult.failure("PostgreSQL query failed: " + e.getMessage());
        }
    }

    private String jdbcUrl(Map<String, Object> credentials) {
        String host = value(credentials, "host", "localhost");
        String port = value(credentials, "port", "5432");
        String database = value(credentials, "database", "");
        String sslMode = value(credentials, "sslMode", "prefer");
        return "jdbc:postgresql://" + host + ":" + port + "/" + database + "?sslmode=" + sslMode;
    }

    private Properties properties(Map<String, Object> credentials) {
        Properties properties = new Properties();
        properties.setProperty("user", value(credentials, "username", ""));
        properties.setProperty("password", value(credentials, "password", ""));
        return properties;
    }

    private String value(Map<String, Object> map, String key, String fallback) {
        Object value = map != null ? map.get(key) : null;
        return value == null ? fallback : String.valueOf(value);
    }

    private int intValue(Object value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private List<?> params(Object value) throws Exception {
        if (value == null) return List.of();
        if (value instanceof List<?> list) return list;
        return objectMapper.readValue(String.valueOf(value), List.class);
    }
}
