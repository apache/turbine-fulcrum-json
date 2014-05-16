package org.apache.fulcrum.json.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;



@JsonTypeInfo(include=As.PROPERTY, use=Id.CLASS, property="type")
public final class TypedRectangle {

// This is only need if no DefaultTyping is set; you have then assign this to object if using collections
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
    public static class Mixins {    }
    
    private int w, h;
    private String name;
 
    public TypedRectangle() {
        // may be is needed for deserialization, if not set otherwise
    }
    
    public TypedRectangle(int w, int h) {
        this.w = w;
        this.h = h;
    }

    public TypedRectangle(int w, int h, String name) {
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