package org.apache.fulcrum.json.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

public final class MixinModule extends SimpleModule {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public final Class<?> clazz;
    public final Class<?> mixin;

    public MixinModule(String name, Class clazz, Class mixin) {
        super(name, Version.unknownVersion());
        this.clazz = clazz;
        this.mixin = mixin;
    }

    @Override
    public void setupModule(SetupContext context) {
        context.setMixInAnnotations(this.clazz, this.mixin);
    }
}