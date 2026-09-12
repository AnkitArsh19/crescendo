package com.crescendo.apps.mongodb;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Real MongoDB database action handlers.
 */
@Component
public class MongoDbHandlers {

    private final ObjectMapper objectMapper;

    public MongoDbHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ActionMapping(appKey = "mongoDb", actionKey = "mongoDb:find")
    public Object find(ActionContext context) throws Exception {
        String collectionName = context.getString("collection");
        if (collectionName == null || collectionName.isBlank()) {
            throw new IllegalArgumentException("Collection name is required for find");
        }

        try (MongoClient client = getClient(context.credentials())) {
            MongoDatabase db = client.getDatabase(getDatabaseName(context));
            MongoCollection<Document> collection = db.getCollection(collectionName);

            Document filter = parseJsonDoc(context.get("query"));
            List<Map<String, Object>> docs = new ArrayList<>();
            collection.find(filter).limit(500).forEach(doc -> docs.add(docToMap(doc)));

            return Map.of(
                    "status", "success",
                    "count", docs.size(),
                    "documents", docs
            );
        }
    }

    @ActionMapping(appKey = "mongoDb", actionKey = "mongoDb:insert")
    public Object insert(ActionContext context) throws Exception {
        String collectionName = context.getString("collection");
        if (collectionName == null || collectionName.isBlank()) {
            throw new IllegalArgumentException("Collection name is required for insert");
        }

        try (MongoClient client = getClient(context.credentials())) {
            MongoDatabase db = client.getDatabase(getDatabaseName(context));
            MongoCollection<Document> collection = db.getCollection(collectionName);

            Document doc = parseJsonDoc(context.get("fields"));
            if (doc.isEmpty()) {
                doc = parseJsonDoc(context.input());
            }
            collection.insertOne(doc);

            return Map.of(
                    "status", "success",
                    "insertedId", doc.get("_id") != null ? String.valueOf(doc.get("_id")) : "",
                    "document", docToMap(doc)
            );
        }
    }

    @ActionMapping(appKey = "mongoDb", actionKey = "mongoDb:update")
    public Object update(ActionContext context) throws Exception {
        String collectionName = context.getString("collection");
        if (collectionName == null || collectionName.isBlank()) {
            throw new IllegalArgumentException("Collection name is required for update");
        }

        try (MongoClient client = getClient(context.credentials())) {
            MongoDatabase db = client.getDatabase(getDatabaseName(context));
            MongoCollection<Document> collection = db.getCollection(collectionName);

            String updateKey = context.getString("updateKey", "_id");
            Object rawQuery = context.get("query");
            Document filter = rawQuery != null ? parseJsonDoc(rawQuery) : new Document();

            Document updateFields = parseJsonDoc(context.get("fields"));
            Document updateDoc = updateFields.containsKey("$set") || updateFields.containsKey("$inc")
                    ? updateFields : new Document("$set", updateFields);

            boolean upsert = context.getBoolean("upsert", false);
            var result = collection.updateMany(filter, updateDoc, new UpdateOptions().upsert(upsert));

            return Map.of(
                    "status", "success",
                    "matchedCount", result.getMatchedCount(),
                    "modifiedCount", result.getModifiedCount(),
                    "upsertedId", result.getUpsertedId() != null ? String.valueOf(result.getUpsertedId()) : ""
            );
        }
    }

    @ActionMapping(appKey = "mongoDb", actionKey = "mongoDb:delete")
    public Object delete(ActionContext context) throws Exception {
        String collectionName = context.getString("collection");
        if (collectionName == null || collectionName.isBlank()) {
            throw new IllegalArgumentException("Collection name is required for delete");
        }

        try (MongoClient client = getClient(context.credentials())) {
            MongoDatabase db = client.getDatabase(getDatabaseName(context));
            MongoCollection<Document> collection = db.getCollection(collectionName);

            Document filter = parseJsonDoc(context.get("query"));
            var result = collection.deleteMany(filter);

            return Map.of(
                    "status", "success",
                    "deletedCount", result.getDeletedCount()
            );
        }
    }

    @ActionMapping(appKey = "mongoDb", actionKey = "mongoDb:aggregate")
    public Object aggregate(ActionContext context) throws Exception {
        String collectionName = context.getString("collection");
        if (collectionName == null || collectionName.isBlank()) {
            throw new IllegalArgumentException("Collection name is required for aggregate");
        }

        try (MongoClient client = getClient(context.credentials())) {
            MongoDatabase db = client.getDatabase(getDatabaseName(context));
            MongoCollection<Document> collection = db.getCollection(collectionName);

            List<Document> pipeline = parsePipeline(context.get("query"));
            List<Map<String, Object>> results = new ArrayList<>();
            collection.aggregate(pipeline).forEach(doc -> results.add(docToMap(doc)));

            return Map.of(
                    "status", "success",
                    "count", results.size(),
                    "results", results
            );
        }
    }

    private MongoClient getClient(Map<String, Object> credentials) {
        String uri = val(credentials, "connectionString", val(credentials, "connectionUri", ""));
        if (uri.isBlank()) {
            throw new IllegalArgumentException("MongoDB connection string is required");
        }
        return MongoClients.create(uri);
    }

    private String getDatabaseName(ActionContext context) {
        String db = val(context.credentials(), "database", "");
        if (db.isBlank()) {
            db = context.getString("database", "test");
        }
        return db;
    }

    private Document parseJsonDoc(Object raw) {
        if (raw == null) return new Document();
        if (raw instanceof Document d) return d;
        if (raw instanceof Map<?, ?> m) {
            return new Document((Map<String, Object>) m);
        }
        String str = String.valueOf(raw).trim();
        if (str.isBlank() || str.equals("{}")) return new Document();
        try {
            return Document.parse(str);
        } catch (Exception e) {
            return new Document();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Document> parsePipeline(Object raw) {
        if (raw == null) return List.of();
        if (raw instanceof List<?> list) {
            List<Document> pipe = new ArrayList<>();
            for (Object item : list) {
                pipe.add(parseJsonDoc(item));
            }
            return pipe;
        }
        String str = String.valueOf(raw).trim();
        if (str.isBlank()) return List.of();
        try {
            List<?> parsed = objectMapper.readValue(str, List.class);
            List<Document> pipe = new ArrayList<>();
            for (Object item : parsed) {
                pipe.add(parseJsonDoc(item));
            }
            return pipe;
        } catch (Exception e) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> docToMap(Document doc) {
        try {
            return objectMapper.readValue(doc.toJson(), Map.class);
        } catch (Exception e) {
            return doc;
        }
    }

    private String val(Map<String, Object> map, String key, String fallback) {
        if (map == null) return fallback;
        Object v = map.get(key);
        return v != null && !String.valueOf(v).isBlank() ? String.valueOf(v) : fallback;
    }
}
