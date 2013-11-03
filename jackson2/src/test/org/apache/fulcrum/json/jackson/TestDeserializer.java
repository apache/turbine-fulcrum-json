package org.apache.fulcrum.json.jackson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.fulcrum.json.Rectangle;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class TestDeserializer extends StdDeserializer<List<Rectangle>> {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    protected TestDeserializer() {
        super(List.class);
        // TODO Auto-generated constructor stub
    }

    @Override
    public List<Rectangle> deserialize(JsonParser jp,
            DeserializationContext ctxt) throws IOException,
            JsonProcessingException {

        ArrayList<Rectangle> list = new ArrayList<Rectangle>();
        // if (jp.getCurrentToken() == JsonToken.START_OBJECT)
        // jp.nextToken(); //
        // START_OBJECT
        while ((jp.nextToken() != JsonToken.END_OBJECT)) {
            String name = null;
            Number size = null;
            int value = 0;
            if (jp.getCurrentToken() == JsonToken.FIELD_NAME) {
                name = jp.getText(); // FIELD_NAME
                jp.nextToken();

            }
            if (jp.getCurrentToken() == JsonToken.VALUE_NUMBER_INT) {
                size = jp.getNumberValue();// size VALUE_NUMBER_INT
                value = (int) Math.sqrt(size.intValue());
            }
            list.add(new Rectangle(value, value, name));
        }
        return list;
    }

}
