package org.apache.fulcrum.json.jackson;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

public class CacheService implements LogEnabled {

    AnnotationIntrospector primary;
    Map<String, FilterProvider> filters =  new ConcurrentHashMap<String, FilterProvider>();;
    
    private static Logger logger;
    
    public CacheService(AnnotationIntrospector primary) {
        this.primary = primary;
    }

    <T> void removeFilter(Class<T> filterClass, Boolean excludeType) {
        if (filterClass == null)
            return;
        if (filters.containsKey(filterClass.getName())) {
            logger.debug("removing filter: " + filterClass.getName());
            removeCustomIntrospectorWithExternalFilterId(filterClass, excludeType);
            SimpleFilterProvider smpfilter = (SimpleFilterProvider) filters
                    .get(filterClass.getName());
            smpfilter.removeFilter(filterClass.getName());
            filters.remove(filterClass.getName());
        }
    }
    
    <T> void removeCustomIntrospectorWithExternalFilterId(
            Class<T> externalFilterId, Boolean excludeType) {
        if (primary instanceof SimpleNameIntrospector) {
            if (externalFilterId != null) {
                ((SimpleNameIntrospector) primary)
                        .removeFilteredClass(externalFilterId);
                if (excludeType) {
                    ((SimpleNameIntrospector) primary)
                    .removeExternalFilterExcludeClass(externalFilterId);
                }
            }
        }
    }

    void cleanSerializerCache(ObjectMapper mapper) {
        if (mapper.getSerializerProvider() instanceof DefaultSerializerProvider) {
            int cachedSerProvs = ((DefaultSerializerProvider) mapper
                    .getSerializerProvider()).cachedSerializersCount();
            if (cachedSerProvs > 0) {
//                getLogger()
//                        .debug("flushing cachedSerializersCount:"
//                                + cachedSerProvs);
                ((DefaultSerializerProvider) mapper.getSerializerProvider())
                        .flushCachedSerializers();
            }
        }
    }

    public Map<String, FilterProvider> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, FilterProvider> filters) {
        this.filters = filters;
    }

    @Override
    public void enableLogging(Logger logger) {
        this.logger = logger;        
    }

}
