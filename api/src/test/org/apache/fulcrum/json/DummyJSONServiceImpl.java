package org.apache.fulcrum.json;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;

public class DummyJSONServiceImpl extends AbstractLogEnabled implements JsonService, Initializable, Configurable
{

    private HashMap<String,String> props;

    @Override
    public String ser(Object src) throws Exception
    {
        return src.toString();
    }

    @Override
    public String ser(Object src, Boolean cleanCache) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> String ser(Object src, Class<T> type) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> String ser(Object src, Class<T> type, Boolean cleanCache) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T deSer(String src, Class<T> type) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> Collection<T> deSerCollection(String json, Object collectionType, Class<T> elementType) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T convertWithFilter(Object src, Class<T> type, String... filterAttrs) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T convertWithFilter(Object src, String... filterAttrs) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String serializeOnlyFilter(Object src, String... filterAttr) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String serializeOnlyFilter(Object src, Boolean cleanFilter, String... filterAttr) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> String serializeOnlyFilter(Object src, Class<T> filterClass, String... filterAttr) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> String serializeOnlyFilter(Object src, Class<T> filterClass, Boolean cleanFilter, String... filterAttr)
            throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> String serializeAllExceptFilter(Object src, Class<T> filterClass, Boolean cleanFilter,
            String... filterAttr) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> String serializeAllExceptFilter(Object src, Class<T> filterClass, String... filterAttr) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String serializeAllExceptFilter(Object src, Boolean cleanFilter, String... filterAttr) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String serializeAllExceptFilter(Object src, String... filterAttr) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JsonService addAdapter(String name, Class target, Class mixin) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JsonService addAdapter(String name, Class target, Object mixin) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setDateFormat(DateFormat df)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void configure(Configuration conf) throws ConfigurationException
    {
        getLogger().debug("conf.getName()" + conf.getName());
        this.props = new HashMap<>();

        final Configuration props = conf.getChild("props", false);

        if (props != null) {
            Configuration[] nameVal = props.getChildren();
            Arrays.stream( nameVal).forEach(c->
            {
                String key = c.getName();
                getLogger().debug("configured key: " + key);
                String val;
                try {
                    val = c.getValue();
                    getLogger().debug("prop " + key + ":" + val);
                    this.props.put(key, val);
                } catch (ConfigurationException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Override
    public void initialize() throws Exception
    {
        // TODO Auto-generated method stub
    }

}
