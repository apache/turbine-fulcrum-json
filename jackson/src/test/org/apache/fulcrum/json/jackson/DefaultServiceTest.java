package org.apache.fulcrum.json.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.fulcrum.json.JsonService;
import org.apache.fulcrum.json.TestClass;
import org.apache.fulcrum.testcontainer.BaseUnit5Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Jackson 2 JSON Test
 * 
 * @author gk
 * @version $Id$
 */
public class DefaultServiceTest extends BaseUnit5Test {
    private JsonService sc = null;
    private final String preDefinedOutput = 
            "{\"container\":{\"cf\":\"Config.xml\"},\"configurationName\":\"Config.xml\",\"name\":\"mytest\"}";

    @BeforeEach
    public void setUp() throws Exception {
        sc = (JsonService) this.lookup(JsonService.ROLE);
    }
    @Test
    public void testSerialize() throws Exception {
        String serJson = sc.ser(new TestClass("mytest"));
        assertEquals(preDefinedOutput, serJson,
                "Serialization of preDefinedOutput failed ");
    }
   
}