package com.crescendo.apps.redis;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Real Redis database action handlers using lightweight RESP protocol over socket.
 */
@Component
public class RedisHandlers {

    @ActionMapping(appKey = "redis", actionKey = "redis:get")
    public Object get(ActionContext context) throws Exception {
        String key = context.getString("key");
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Key is required");
        Object val = executeCommand(context.credentials(), List.of("GET", key));
        return Map.of("status", "success", "key", key, "value", val != null ? val : "");
    }

    @ActionMapping(appKey = "redis", actionKey = "redis:set")
    public Object set(ActionContext context) throws Exception {
        String key = context.getString("key");
        String value = context.getString("value", "");
        int ttl = context.getInt("ttl", 0);
        boolean expire = context.getBoolean("expire", false);

        if (key == null || key.isBlank()) throw new IllegalArgumentException("Key is required");

        List<String> cmd = new ArrayList<>();
        cmd.add("SET");
        cmd.add(key);
        cmd.add(value);
        if (expire && ttl > 0) {
            cmd.add("EX");
            cmd.add(String.valueOf(ttl));
        }

        Object result = executeCommand(context.credentials(), cmd);
        return Map.of("status", "success", "key", key, "result", result != null ? result : "OK");
    }

    @ActionMapping(appKey = "redis", actionKey = "redis:delete")
    public Object delete(ActionContext context) throws Exception {
        String key = context.getString("key");
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Key is required");
        Object deletedCount = executeCommand(context.credentials(), List.of("DEL", key));
        return Map.of("status", "success", "key", key, "deleted", deletedCount != null ? deletedCount : 0);
    }

    @ActionMapping(appKey = "redis", actionKey = "redis:incr")
    public Object incr(ActionContext context) throws Exception {
        String key = context.getString("key");
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Key is required");
        Object newValue = executeCommand(context.credentials(), List.of("INCR", key));
        int ttl = context.getInt("ttl", 0);
        if (context.getBoolean("expire", false) && ttl > 0) {
            executeCommand(context.credentials(), List.of("EXPIRE", key, String.valueOf(ttl)));
        }
        return Map.of("status", "success", "key", key, "value", newValue != null ? newValue : 0);
    }

    @ActionMapping(appKey = "redis", actionKey = "redis:info")
    public Object info(ActionContext context) throws Exception {
        Object info = executeCommand(context.credentials(), List.of("INFO"));
        return Map.of("status", "success", "info", info != null ? info : "");
    }

    @ActionMapping(appKey = "redis", actionKey = "redis:keys")
    public Object keys(ActionContext context) throws Exception {
        String pattern = context.getString("keyPattern", "*");
        if (pattern == null || pattern.isBlank()) pattern = "*";
        Object keysList = executeCommand(context.credentials(), List.of("KEYS", pattern));
        return Map.of("status", "success", "pattern", pattern, "keys", keysList != null ? keysList : List.of());
    }

    @ActionMapping(appKey = "redis", actionKey = "redis:llen")
    public Object llen(ActionContext context) throws Exception {
        String list = context.getString("list");
        if (list == null || list.isBlank()) throw new IllegalArgumentException("List name is required");
        Object length = executeCommand(context.credentials(), List.of("LLEN", list));
        return Map.of("status", "success", "list", list, "length", length != null ? length : 0);
    }

    @ActionMapping(appKey = "redis", actionKey = "redis:pop")
    public Object pop(ActionContext context) throws Exception {
        String list = context.getString("list");
        if (list == null || list.isBlank()) throw new IllegalArgumentException("List name is required");
        boolean tail = context.getBoolean("tail", false);
        String op = tail ? "RPOP" : "LPOP";
        Object item = executeCommand(context.credentials(), List.of(op, list));
        return Map.of("status", "success", "list", list, "item", item != null ? item : "");
    }

    @ActionMapping(appKey = "redis", actionKey = "redis:push")
    public Object push(ActionContext context) throws Exception {
        String list = context.getString("list");
        String data = context.getString("messageData", "");
        if (list == null || list.isBlank()) throw new IllegalArgumentException("List name is required");
        boolean tail = context.getBoolean("tail", false);
        String op = tail ? "RPUSH" : "LPUSH";
        Object length = executeCommand(context.credentials(), List.of(op, list, data));
        return Map.of("status", "success", "list", list, "newLength", length != null ? length : 0);
    }

    @ActionMapping(appKey = "redis", actionKey = "redis:publish")
    public Object publish(ActionContext context) throws Exception {
        String channel = context.getString("channel");
        String data = context.getString("messageData", "");
        if (channel == null || channel.isBlank()) throw new IllegalArgumentException("Channel is required");
        Object subscribers = executeCommand(context.credentials(), List.of("PUBLISH", channel, data));
        return Map.of("status", "success", "channel", channel, "subscribers", subscribers != null ? subscribers : 0);
    }

    private Object executeCommand(Map<String, Object> credentials, List<String> command) throws Exception {
        String host = val(credentials, "host", "localhost");
        int port = 6379;
        try {
            port = Integer.parseInt(val(credentials, "port", "6379"));
        } catch (NumberFormatException ignored) {}
        String password = val(credentials, "password", "");

        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(10_000);
            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

            if (!password.isBlank()) {
                send(out, List.of("AUTH", password));
                read(in);
            }

            send(out, command);
            return read(in);
        }
    }

    private void send(BufferedOutputStream out, List<String> parts) throws Exception {
        StringBuilder frame = new StringBuilder("*").append(parts.size()).append("\r\n");
        for (String part : parts) {
            byte[] bytes = (part != null ? part : "").getBytes(StandardCharsets.UTF_8);
            frame.append("$").append(bytes.length).append("\r\n").append(part != null ? part : "").append("\r\n");
        }
        out.write(frame.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private Object read(BufferedInputStream in) throws Exception {
        int prefix = in.read();
        if (prefix == -1) throw new IllegalStateException("No response from Redis");
        return switch ((char) prefix) {
            case '+' -> readLine(in);
            case '-' -> throw new IllegalStateException(readLine(in));
            case ':' -> Long.parseLong(readLine(in));
            case '$' -> bulk(in);
            case '*' -> array(in);
            default -> throw new IllegalStateException("Unknown Redis response prefix: " + (char) prefix);
        };
    }

    private String bulk(BufferedInputStream in) throws Exception {
        int len = Integer.parseInt(readLine(in));
        if (len < 0) return null;
        byte[] bytes = in.readNBytes(len);
        in.readNBytes(2);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private List<Object> array(BufferedInputStream in) throws Exception {
        int len = Integer.parseInt(readLine(in));
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < len; i++) values.add(read(in));
        return values;
    }

    private String readLine(BufferedInputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\r') {
                in.read();
                break;
            }
            sb.append((char) b);
        }
        return sb.toString();
    }

    private String val(Map<String, Object> map, String key, String fallback) {
        if (map == null) return fallback;
        Object v = map.get(key);
        return v != null ? String.valueOf(v) : fallback;
    }
}
