package com.crescendo.apps.mysql;

import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import java.sql.*;
import java.util.*;

@ActionMapping(appKey = "mysql", actionKey = "execute-query")
public class MySQLExecuteQueryHandler implements ActionHandler {
    private final ObjectMapper mapper;
    public MySQLExecuteQueryHandler(ObjectMapper mapper) { this.mapper = mapper; }
    @Override
public ActionResult execute(ActionContext c) {
        try (Connection conn = DriverManager.getConnection(url(c), str(c.credentials(), "username", ""), str(c.credentials(), "password", ""));
             PreparedStatement ps = conn.prepareStatement(str(c.configuration(), "sql", ""))) {
            if (str(c.configuration(), "sql", "").isBlank()) return ActionResult.failure("MySQL SQL is required");
            List<?> params = params(c.configuration().get("params"));
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            if (!ps.execute()) return ActionResult.success(Map.of("updatedRows", ps.getUpdateCount()));
            List<Map<String,Object>> rows = new ArrayList<>();
            int max = Math.max(1, intValue(c.configuration().get("maxRows"), 100));
            try (ResultSet rs = ps.getResultSet()) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next() && rows.size() < max) {
                    Map<String,Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) row.put(meta.getColumnLabel(i), rs.getObject(i));
                    rows.add(row);
                }
            }
            return ActionResult.success(Map.of("rows", rows, "count", rows.size()));
        } catch (Exception e) { return ActionResult.failure("MySQL query failed: " + e.getMessage()); }
    }
    private String url(ActionContext c) { return "jdbc:mysql://" + str(c.credentials(),"host","localhost") + ":" + str(c.credentials(),"port","3306") + "/" + str(c.credentials(),"database","") + "?useSSL=true&serverTimezone=UTC"; }
    private String str(Map<String,Object> m,String k,String f){Object v=m!=null?m.get(k):null;return v==null?f:String.valueOf(v);}
    private int intValue(Object v,int f){try{return v==null?f:Integer.parseInt(String.valueOf(v));}catch(Exception e){return f;}}
    private List<?> params(Object v) throws Exception { if (v == null) return List.of(); if (v instanceof List<?> l) return l; return mapper.readValue(String.valueOf(v), List.class); }
}
