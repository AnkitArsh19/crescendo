package com.crescendo.apps.mysql;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Real MySQL database action handlers.
 */
@Component
public class MySqlHandlers {

    @ActionMapping(appKey = "mysql", actionKey = "executeQuery")
    public Object executeQuery(ActionContext context) throws Exception {
        String sql = context.getString("query");
        if (sql == null || sql.isBlank()) {
            sql = context.getString("sql");
        }
        if (sql == null || sql.isBlank()) {
            Object inputQuery = context.input("query");
            if (inputQuery != null) sql = String.valueOf(inputQuery);
        }
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL query is required");
        }

        try (Connection connection = getConnection(context.credentials());
             Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30);
            boolean hasResultSet = statement.execute(sql);

            if (!hasResultSet) {
                return Map.of(
                        "status", "success",
                        "updatedRows", statement.getUpdateCount()
                );
            }

            try (ResultSet rs = statement.getResultSet()) {
                List<Map<String, Object>> rows = extractRows(rs, 500);
                return Map.of(
                        "status", "success",
                        "count", rows.size(),
                        "rows", rows
                );
            }
        }
    }

    @ActionMapping(appKey = "mysql", actionKey = "select")
    public Object select(ActionContext context) throws Exception {
        String table = context.getString("table");
        if (table == null || table.isBlank()) {
            throw new IllegalArgumentException("Table name is required for select");
        }
        String columns = context.getString("columns", "*");
        if (columns == null || columns.isBlank()) columns = "*";

        String sql = "SELECT " + columns + " FROM " + sanitizeIdentifier(table);
        String selectBy = context.getString("selectBy");
        String id = context.getString("id");
        if (selectBy != null && !selectBy.equalsIgnoreCase("all") && id != null && !id.isBlank()) {
            sql += " WHERE " + sanitizeIdentifier(selectBy) + " = ?";
        }

        try (Connection connection = getConnection(context.credentials())) {
            if (selectBy != null && !selectBy.equalsIgnoreCase("all") && id != null && !id.isBlank()) {
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setObject(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        List<Map<String, Object>> rows = extractRows(rs, 500);
                        return Map.of("status", "success", "count", rows.size(), "rows", rows);
                    }
                }
            } else {
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    List<Map<String, Object>> rows = extractRows(rs, 500);
                    return Map.of("status", "success", "count", rows.size(), "rows", rows);
                }
            }
        }
    }

    @ActionMapping(appKey = "mysql", actionKey = "insert")
    public Object insert(ActionContext context) throws Exception {
        String table = context.getString("table");
        if (table == null || table.isBlank()) {
            throw new IllegalArgumentException("Table name is required for insert");
        }
        String sql = context.getString("columns");
        if (sql == null || sql.isBlank()) {
            return Map.of("status", "success", "message", "Insert completed");
        }
        try (Connection connection = getConnection(context.credentials());
             Statement stmt = connection.createStatement()) {
            String insertSql = "INSERT INTO " + sanitizeIdentifier(table) + " " + sql;
            int affected = stmt.executeUpdate(insertSql);
            return Map.of("status", "success", "updatedRows", affected);
        }
    }

    @ActionMapping(appKey = "mysql", actionKey = "update")
    public Object update(ActionContext context) throws Exception {
        String table = context.getString("table");
        if (table == null || table.isBlank()) {
            throw new IllegalArgumentException("Table name is required for update");
        }
        String columns = context.getString("columns");
        String updateBy = context.getString("updateBy", "id");
        String id = context.getString("id");

        if (columns == null || columns.isBlank()) {
            return Map.of("status", "success", "message", "Update completed");
        }

        String sql = "UPDATE " + sanitizeIdentifier(table) + " SET " + columns;
        if (id != null && !id.isBlank()) {
            sql += " WHERE " + sanitizeIdentifier(updateBy) + " = ?";
        }

        try (Connection connection = getConnection(context.credentials())) {
            if (id != null && !id.isBlank()) {
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setObject(1, id);
                    int affected = ps.executeUpdate();
                    return Map.of("status", "success", "updatedRows", affected);
                }
            } else {
                try (Statement stmt = connection.createStatement()) {
                    int affected = stmt.executeUpdate(sql);
                    return Map.of("status", "success", "updatedRows", affected);
                }
            }
        }
    }

    @ActionMapping(appKey = "mysql", actionKey = "upsert")
    public Object upsert(ActionContext context) throws Exception {
        return update(context);
    }

    @ActionMapping(appKey = "mysql", actionKey = "deleteTable")
    public Object deleteTable(ActionContext context) throws Exception {
        String table = context.getString("table");
        if (table == null || table.isBlank()) {
            throw new IllegalArgumentException("Table name is required for delete");
        }
        String deleteBy = context.getString("deleteBy", "id");
        String id = context.getString("id");

        String sql = "DELETE FROM " + sanitizeIdentifier(table);
        if (id != null && !id.isBlank()) {
            sql += " WHERE " + sanitizeIdentifier(deleteBy) + " = ?";
        }

        try (Connection connection = getConnection(context.credentials())) {
            if (id != null && !id.isBlank()) {
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setObject(1, id);
                    int affected = ps.executeUpdate();
                    return Map.of("status", "success", "deletedRows", affected);
                }
            } else {
                try (Statement stmt = connection.createStatement()) {
                    int affected = stmt.executeUpdate(sql);
                    return Map.of("status", "success", "deletedRows", affected);
                }
            }
        }
    }

    private Connection getConnection(Map<String, Object> credentials) throws Exception {
        String host = val(credentials, "host", "localhost");
        String port = val(credentials, "port", "3306");
        String database = val(credentials, "database", "");
        String user = val(credentials, "username", val(credentials, "user", "root"));
        String password = val(credentials, "password", "");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?connectTimeout=10000&socketTimeout=30000";
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);
        return DriverManager.getConnection(url, props);
    }

    private List<Map<String, Object>> extractRows(ResultSet rs, int maxRows) throws Exception {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next() && rows.size() < maxRows) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= colCount; i++) {
                row.put(meta.getColumnLabel(i), rs.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }

    private String val(Map<String, Object> map, String key, String fallback) {
        if (map == null) return fallback;
        Object v = map.get(key);
        return v != null ? String.valueOf(v) : fallback;
    }

    private String sanitizeIdentifier(String id) {
        if (id == null || id.isBlank()) return "";
        return id.replaceAll("[^a-zA-Z0-9_]", "");
    }
}
