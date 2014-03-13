package org.apache.fulcrum.json.gson;

import java.lang.reflect.Type;

import org.apache.fulcrum.json.Rectangle;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class TypeAdapterForRectangle implements JsonSerializer<Rectangle> {

    @Override
    public JsonElement serialize(Rectangle src, Type typeOfSrc,
            JsonSerializationContext context) {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", src.getName());
        jsonObject.addProperty("width", src.getW());
        // ignore rest
        return jsonObject;
    }
    

}
