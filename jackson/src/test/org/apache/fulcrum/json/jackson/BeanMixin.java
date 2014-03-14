package org.apache.fulcrum.json.jackson;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;



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