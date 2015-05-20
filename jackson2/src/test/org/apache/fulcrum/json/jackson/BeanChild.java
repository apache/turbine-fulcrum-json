package org.apache.fulcrum.json.jackson;

public class BeanChild extends Bean {
    private String name;
    private int height;
    public String type;

    public BeanChild() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
