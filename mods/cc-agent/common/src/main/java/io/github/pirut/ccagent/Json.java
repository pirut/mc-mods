package io.github.pirut.ccagent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.HashMap;
import java.util.Map;

/** JSON helpers: tool schemas, argument reading, and turning JSON into Lua values. */
public final class Json {
    public static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final Gson COMPACT = new GsonBuilder().disableHtmlEscaping().create();

    private Json() {
    }

    public static JsonObject obj(Object... pairs) {
        var out = new JsonObject();
        for (int i = 0; i < pairs.length; i += 2) {
            var value = pairs[i + 1];
            if (value instanceof JsonElement e) out.add((String) pairs[i], e);
            else if (value instanceof Number n) out.addProperty((String) pairs[i], n);
            else if (value instanceof Boolean b) out.addProperty((String) pairs[i], b);
            else if (value != null) out.addProperty((String) pairs[i], value.toString());
        }
        return out;
    }

    public static JsonArray arr(String... items) {
        var out = new JsonArray();
        for (var item : items) out.add(item);
        return out;
    }

    public static JsonObject prop(String type, String description) {
        return obj("type", type, "description", description);
    }

    public static JsonObject schema(JsonObject properties, String... required) {
        return obj("type", "object", "properties", properties, "required", arr(required), "additionalProperties", false);
    }

    public static String str(JsonObject args, String key, String fallback) {
        var value = args.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsString();
    }

    public static String need(JsonObject args, String key) {
        var value = str(args, key, null);
        if (value == null) throw new ToolException("missing " + key);
        return value;
    }

    public static int integer(JsonObject args, String key, int fallback) {
        var value = args.get(key);
        if (value == null || value.isJsonNull()) return fallback;
        try {
            return (int) Math.round(value.getAsDouble());
        } catch (RuntimeException e) {
            throw new ToolException(key + " must be a number");
        }
    }

    public static boolean bool(JsonObject args, String key, boolean fallback) {
        var value = args.get(key);
        if (value == null || value.isJsonNull()) return fallback;
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) return Boolean.parseBoolean(value.getAsString());
        return value.getAsBoolean();
    }

    public static JsonArray array(JsonObject args, String key) {
        var value = args.get(key);
        if (value == null || value.isJsonNull()) return new JsonArray();
        if (value.isJsonArray()) return value.getAsJsonArray();
        // Some clients send arrays as JSON text.
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            try {
                var parsed = com.google.gson.JsonParser.parseString(value.getAsString());
                if (parsed.isJsonArray()) return parsed.getAsJsonArray();
            } catch (RuntimeException ignored) {
            }
        }
        var single = new JsonArray();
        single.add(value);
        return single;
    }

    /** A Lua expression for this JSON value. */
    public static String lua(JsonElement value) {
        if (value == null || value.isJsonNull()) return "nil";
        if (value.isJsonPrimitive()) {
            JsonPrimitive p = value.getAsJsonPrimitive();
            if (p.isBoolean()) return Boolean.toString(p.getAsBoolean());
            if (p.isNumber()) {
                double d = p.getAsDouble();
                return d == Math.rint(d) && Math.abs(d) < 1e15 ? Long.toString((long) d) : Double.toString(d);
            }
            return LuaStrings.quote(p.getAsString());
        }
        var out = new StringBuilder("{");
        if (value.isJsonArray()) {
            for (var item : value.getAsJsonArray()) out.append(lua(item)).append(", ");
        } else {
            for (var entry : value.getAsJsonObject().entrySet()) {
                out.append('[').append(LuaStrings.quote(entry.getKey())).append("] = ").append(lua(entry.getValue())).append(", ");
            }
        }
        return out.append('}').toString();
    }

    /** A Java value CC: Tweaked can pass to Lua (for events). */
    public static Object java(JsonElement value) {
        if (value == null || value.isJsonNull()) return null;
        if (value.isJsonPrimitive()) {
            var p = value.getAsJsonPrimitive();
            if (p.isBoolean()) return p.getAsBoolean();
            if (p.isNumber()) return p.getAsDouble();
            return LuaStrings.toLua(p.getAsString());
        }
        Map<Object, Object> table = new HashMap<>();
        if (value.isJsonArray()) {
            int i = 1;
            for (var item : value.getAsJsonArray()) {
                var v = java(item);
                if (v != null) table.put((double) i, v);
                i++;
            }
        } else {
            for (var entry : value.getAsJsonObject().entrySet()) {
                var v = java(entry.getValue());
                if (v != null) table.put(LuaStrings.toLua(entry.getKey()), v);
            }
        }
        return table;
    }
}
