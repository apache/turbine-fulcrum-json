package org.apache.fulcrum.json.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class BeanMixin {
    BeanMixin() {
    }

    @JsonIgnore
    abstract int getAge();

    @JsonIgnore
    String profession; // exclude

    @JsonProperty
    abstract String getName();//
}