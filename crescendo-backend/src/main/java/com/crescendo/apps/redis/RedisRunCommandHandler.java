package com.crescendo.apps.redis;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ActionMapping(appKey = "redis", actionKey = "run-command")
public class RedisRunCommandHandler implements ActionHandler {

    private final ObjectMapper objectMapper;

    public RedisRunCommandHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
public ActionResult execute(ActionContext context) {
        try (Socket socket = new Socket(value(context.credentials(), "host", "localhost"),
                intValue(value(context.credentials(), "port", "6379"), 6379))) {
            socket.setSoTimeout(10_000);
            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

            String password = value(context.credentials(), "password", "");
            if (!password.isBlank()) {
                send(out, List.of("AUTH", password));
                read(in);
            }
            String database = value(context.credentials(), "database", "");
            if (!database.isBlank()) {
                send(out, List.of("SELECT", database));
                read(in);
            }

            List<String> command = commandParts(context);
            if (command.isEmpty()) return ActionResult.failure("Redis command is required");
            send(out, command);
            Object result = read(in);
            return ActionResult.success(Map.of("result", result != null ? result : "", "command", command));
        } catch (Exception e) {
            return ActionResult.failure("Redis command failed: " + e.getMessage());
        }
    }

    private List<String> commandParts(ActionContext context) throws Exception {
        Object args = context.configuration().get("args");
        if (args != null) {
            List<?> values = args instanceof List<?> list ? list : objectMapper.readValue(String.valueOf(args), List.class);
            List<String> parts = new ArrayList<>();
            for (Object value : values) parts.add(String.valueOf(value));
            return parts;
        }
        String command = value(context.configuration(), "command", "");
        if (command.isBlank()) return List.of();
        return List.of(command.trim().split("\\s+"));
    }

    private void send(BufferedOutputStream out, List<String> parts) throws Exception {
        StringBuilder frame = new StringBuilder("*").append(parts.size()).append("\r\n");
        for (String part : parts) {
            byte[] bytes = part.getBytes(StandardCharsets.UTF_8);
            frame.append("$").append(bytes.length).append("\r\n").append(part).append("\r\n");
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

    private String value(Map<String, Object> map, String key, String fallback) {
        Object value = map != null ? map.get(key) : null;
        return value == null ? fallback : String.valueOf(value);
    }

    private int intValue(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
