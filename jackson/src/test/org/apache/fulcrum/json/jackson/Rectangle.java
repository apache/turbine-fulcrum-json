package org.apache.fulcrum.json.jackson;

import org.codehaus.jackson.annotate.JsonIgnore;

public final class Rectangle {
    private int w, h;
    private String name;
 
    public Rectangle() {
        // may be this is needed for deserialization, if not set otherwise
    }
    
    public Rectangle(int w, int h) {
        this.w = w;
        this.h = h;
    }

    public Rectangle(int w, int h, String name) {
        this.w = w;
        this.h = h;
        this.name = name;
    }

    public int getW() {
        return w;
    }

    public int getH() {
        return h;
    }

    @JsonIgnore
    public int getSize() {
        return w * h;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}