package org.apache.fulcrum.json.gson;

import java.lang.reflect.Type;

import org.apache.fulcrum.json.TestClass;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class TestJsonSerializer implements JsonSerializer<TestClass> {
    
    public TestJsonSerializer()
    {
    }

    @Override
    public JsonElement serialize( TestClass src, Type typeOfSrc, JsonSerializationContext context )
    {
        final JsonObject json = new JsonObject();
        json.addProperty("n", src.getName());
        json.addProperty("p", src.getConfigurationName());

        final JsonArray categoriesArray = new JsonArray();
        json.add("c", categoriesArray);
        return json;
    }
  }