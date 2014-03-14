package org.apache.fulcrum.json.jackson.filters;

import org.apache.fulcrum.json.jackson.Jackson2MapperService;
import org.apache.fulcrum.json.jackson.Jackson2MapperService.CustomModule;

import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Used by {@link Jackson2MapperService#addAdapter(String, Class, Object)} to provide a wrapper module as a helper class
 * for the inner class {@link CustomModule}.
 * @author gkallidis
 *
 * @param <T>
 */
public class CustomModuleWrapper<T> {
    StdSerializer<T> ser;
    StdDeserializer<T> deSer;

    public CustomModuleWrapper(StdSerializer<T> ser, StdDeserializer<T> deSer) {
        this.ser = ser;
        this.deSer = deSer;
    }

    public StdSerializer<T> getSer() {
        return ser;
    }

    public void setSer(StdSerializer<T> ser) {
        this.ser = ser;
    }

    public StdDeserializer<T> getDeSer() {
        return deSer;
    }

    public void setDeSer(StdDeserializer<T> deSer) {
        this.deSer = deSer;
    }

}
