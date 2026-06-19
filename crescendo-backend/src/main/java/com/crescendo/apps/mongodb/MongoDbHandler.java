package com.crescendo.apps.mongodb;

import com.crescendo.execution.action.*;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;
import java.util.*;

@SuppressWarnings("unchecked")
abstract class MongoDbHandler implements ActionHandler {
    private final ObjectMapper mapper;
    MongoDbHandler(ObjectMapper mapper){this.mapper=mapper;}

    ActionResult findDocuments(ActionContext c, Map<String, Object> filter, int limit) {
        if (hasDirectUri(c)) {
            return directFind(c, filter, limit);
        }
        Map<String,Object> body = base(c);
        body.put("filter", filter);
        body.put("limit", limit);
        return post(c, "find", body);
    }

    ActionResult insertOneDocument(ActionContext c, Map<String, Object> document) {
        if (hasDirectUri(c)) {
            return directInsertOne(c, document);
        }
        Map<String,Object> body = base(c);
        body.put("document", document);
        return post(c, "insertOne", body);
    }

    ActionResult post(ActionContext c, String op, Map<String,Object> body) {
        try {
            String res = RestClient.builder().baseUrl(trim(cred(c,"endpoint")))
                    .defaultHeader("api-key", cred(c,"apiKey")).defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE).build()
                    .post().uri("/action/" + op).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(String.class);
            return ActionResult.success(Map.of("data", mapper.readValue(res, Object.class), "raw", res));
        } catch (Exception e) { return ActionResult.failure("MongoDB request failed: " + e.getMessage()); }
    }

    private ActionResult directFind(ActionContext c, Map<String, Object> filter, int limit) {
        try (var client = MongoClients.create(cred(c, "connectionUri"))) {
            var collection = client.getDatabase(val(c, "database"))
                    .getCollection(val(c, "collection"));
            Bson query = filter == null || filter.isEmpty() ? Filters.empty() : toDocument(filter);
            List<Map<String, Object>> docs = new ArrayList<>();
            collection.find(query)
                    .sort(Sorts.descending("_id"))
                    .limit(Math.max(1, Math.min(limit, 500)))
                    .forEach(doc -> docs.add(documentToMap(doc)));
            return ActionResult.success(Map.of("documents", docs, "count", docs.size(), "mode", "direct"));
        } catch (Exception e) {
            return ActionResult.failure("MongoDB direct find failed: " + e.getMessage());
        }
    }

    private ActionResult directInsertOne(ActionContext c, Map<String, Object> document) {
        try (var client = MongoClients.create(cred(c, "connectionUri"))) {
            Document doc = toDocument(document != null ? document : Map.of());
            client.getDatabase(val(c, "database"))
                    .getCollection(val(c, "collection"))
                    .insertOne(doc);
            return ActionResult.success(Map.of(
                    "insertedId", doc.get("_id") != null ? String.valueOf(doc.get("_id")) : "",
                    "document", documentToMap(doc),
                    "mode", "direct"));
        } catch (Exception e) {
            return ActionResult.failure("MongoDB direct insert failed: " + e.getMessage());
        }
    }

    private Document toDocument(Map<String, Object> value) throws Exception {
        return Document.parse(mapper.writeValueAsString(value));
    }

    private Map<String, Object> documentToMap(Document document) {
        try {
            return mapper.readValue(document.toJson(), Map.class);
        } catch (Exception e) {
            return Map.of("raw", document.toJson());
        }
    }

    Map<String,Object> base(ActionContext c){Map<String,Object>b=new LinkedHashMap<>();b.put("dataSource",cred(c,"dataSource"));b.put("database",val(c,"database"));b.put("collection",val(c,"collection"));return b;}
    Object json(Object v,Object f)throws Exception{if(v==null)return f;if(v instanceof Map<?,?> || v instanceof List<?>)return v;return mapper.readValue(String.valueOf(v),Object.class);}
    Map<String,Object> jsonMap(Object v)throws Exception{Object parsed=json(v,Map.of());return parsed instanceof Map<?,?> m?(Map<String,Object>)m:Map.of();}
    String val(ActionContext c,String k){Object v=c.configuration().get(k);return v==null?"":String.valueOf(v);}
    String cred(ActionContext c,String k){Object v=c.credentials()!=null?c.credentials().get(k):null;return v==null?"":String.valueOf(v);}
    boolean hasDirectUri(ActionContext c){String uri=cred(c,"connectionUri");return uri.startsWith("mongodb://")||uri.startsWith("mongodb+srv://");}
    String trim(String s){return s.endsWith("/")?s.substring(0,s.length()-1):s;}
    int intVal(Object v,int f){try{return v==null?f:Integer.parseInt(String.valueOf(v));}catch(Exception e){return f;}}
}
