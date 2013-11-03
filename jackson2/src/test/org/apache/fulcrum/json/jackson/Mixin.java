package org.apache.fulcrum.json.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class Mixin {
    void MixIn(int w, int h) {
    }

    @JsonProperty("width")
    abstract int getW(); // rename property

    @JsonIgnore
    abstract int getH();

    @JsonIgnore
    abstract int getSize(); // exclude

    @JsonIgnore
    abstract String getName();
}
