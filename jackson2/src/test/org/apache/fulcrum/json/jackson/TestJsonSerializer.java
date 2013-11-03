package org.apache.fulcrum.json.jackson;

import java.io.IOException;

import org.apache.fulcrum.json.TestClass;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class TestJsonSerializer extends StdSerializer<TestClass> {

    protected TestJsonSerializer() {
        super(TestClass.class, false);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void serialize(TestClass value, JsonGenerator jgen,
            SerializerProvider provider) throws IOException,
            JsonGenerationException {
        jgen.writeStartObject();
        jgen.writeFieldName("n");
        jgen.writeString(value.getName());
        jgen.writeFieldName("p");
        jgen.writeString(value.getConfigurationName());
        jgen.writeArrayFieldStart("c");
        jgen.writeEndArray();
        jgen.writeEndObject();
    }

}