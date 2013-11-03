package org.apache.fulcrum.json.jackson;

import java.io.IOException;
import java.util.List;

import org.apache.fulcrum.json.Rectangle;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class TestSerializer extends StdSerializer<List<Rectangle>> {

    protected TestSerializer() {
        super(List.class, false);
    }

    @Override
    public void serialize(List<Rectangle> data, JsonGenerator jgen,
            SerializerProvider provider) throws IOException,
            JsonGenerationException {
        jgen.writeStartObject();
        for (int i = 0; i < data.size(); i++) {
            jgen.writeFieldName(data.get(i).getName());
            jgen.writeNumber(data.get(i).getSize());
        }
        jgen.writeEndObject();

    }
}
