package org.apache.fulcrum.json.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public final class CustomModule<T> extends SimpleModule {

    private static final long serialVersionUID = 1L;

    public CustomModule(String name, Class<T> targetClazz,
            StdSerializer<T> stdSer, StdDeserializer<T> stdDeser) {
        super(name, Version.unknownVersion());
        addSerializer(targetClazz, stdSer);
        addDeserializer(targetClazz, stdDeser);
    }
}
