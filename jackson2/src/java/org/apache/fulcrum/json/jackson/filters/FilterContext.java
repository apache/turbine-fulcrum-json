package org.apache.fulcrum.json.jackson.filters;

import com.fasterxml.jackson.databind.ser.BeanPropertyFilter;

public class FilterContext {

    BeanPropertyFilter filter;

    public BeanPropertyFilter getFilter() {
        return filter;
    }

    public void setFilter(BeanPropertyFilter bpf) {
        this.filter = bpf;
    }

}
