package org.apache.fulcrum.json;

import java.util.Map;

public class TestClass

{
    /** Container for the components */
    private Map<String, Object> container;
    /** Setup our default configurationFileName */
    private String configurationName = "Config.xml";

    /** Setup our default parameterFileName */
    private String name = null;
    
    public TestClass()
    {
        // TODO Auto-generated constructor stub
    }
    
    public TestClass(String name) {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getConfigurationName()
    {
        return configurationName;
    }

    public void setConfigurationName( String configurationName )
    {
        this.configurationName = configurationName;
    }


   
}

