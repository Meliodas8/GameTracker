package dev.manel.gametracker.session;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.Instant;

public class InstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {

    @Override
    public JsonElement serialize(Instant src, Type type, JsonSerializationContext ctx) {
        return new JsonPrimitive(src.toString());
    }

    @Override
    public Instant deserialize(JsonElement json, Type type, JsonDeserializationContext ctx) {
        return Instant.parse(json.getAsString());
    }
}
