package com.voidvvv.kzcollision.core.serialization;

import com.google.gson.*;
import com.voidvvv.kzcollision.core.model.Rect;

import java.lang.reflect.Type;

public class ProjectGsonFactory {

    public static Gson create() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Rect.class, new RectSerializer())
                .registerTypeAdapter(Rect.class, new RectDeserializer())
                .create();
    }

    private static class RectSerializer implements JsonSerializer<Rect> {
        @Override
        public JsonElement serialize(Rect src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("x", src.x);
            obj.addProperty("y", src.y);
            obj.addProperty("width", src.width);
            obj.addProperty("height", src.height);
            return obj;
        }
    }

    private static class RectDeserializer implements JsonDeserializer<Rect> {
        @Override
        public Rect deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            return new Rect(
                obj.get("x").getAsFloat(),
                obj.get("y").getAsFloat(),
                obj.get("width").getAsFloat(),
                obj.get("height").getAsFloat()
            );
        }
    }
}
