package org.apache.fulcrum.json.jackson;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * 
 * @author gk
 * @version $Id$
 * 
 */
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
