package org.apache.fulcrum.json.jackson.mixins;

import org.apache.fulcrum.json.jackson.example.Bean;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class BeanMixin2 extends Bean {
    BeanMixin2() {
    }
    @Override
    @JsonIgnore
    public abstract String getName();//
}
