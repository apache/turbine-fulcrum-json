package org.apache.fulcrum.json.gson;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fulcrum.json.JsonService;
import org.apache.fulcrum.json.Rectangle;
import org.apache.fulcrum.json.TestClass;
import org.apache.fulcrum.testcontainer.BaseUnit4Test;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonPrimitive;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;

/**
 * GSON JSON Test
 *
 * @author gk
 * @version $Id$
 */
public class JsonPathGSONServiceTest extends BaseUnit4Test
{
    private JsonService sc = null;
    private final String preDefinedOutput = 
        "{\"container\":{\"cf\":\"Config.xml\"},\"configurationName\":\"Config.xml\",\"name\":\"mytest\"}";

    @Before
    public void setUp() throws Exception
    {
        sc = (JsonService) this.lookup( JsonService.ROLE );
    }
    @Test
    public void testSerialize() throws Exception
    {
        String serJson = sc.ser( new TestClass("mytest") );
        String cf = ((JsonPrimitive) JsonPath.parse(serJson).read("$.container.cf")).getAsString();// .using(conf)
        assertEquals("Serialization failed ", "Config.xml", cf);   
    }
    
    @Test
    public void testDefaultGsonSerializeDate() throws Exception
    {
        // default calls with default DateTypeAdapter
        Map<String,Object> map = new HashMap<String,Object>();
        Calendar refDate = Calendar.getInstance();
        map.put( "date", refDate.getTime() );

        //sc.setDateFormat( MMddyyyy );
        String serJson = sc.ser( map );  
        System.out.println("serJson:"+ serJson);
        assertTrue("Serialize with Adapater failed ", serJson.matches( "\\{\"date\":\"\\d\\d:\\d\\d:\\d{4}\"\\}" ));

        Date date = JsonPath.parse(serJson).read("$.date", Date.class);// .using(conf)
        Calendar parsedDate = Calendar.getInstance();
        parsedDate.setTime(date);
        assertEquals("Serialization failed ", refDate.get(Calendar.DATE), parsedDate.get(Calendar.DATE));  
    }
    
    @Test
    public void testGsonSerializeDate() throws Exception
    {
        final SimpleDateFormat MMddyyyy = new SimpleDateFormat( "MM/dd/yyyy");
        Map<String,Object> map = new HashMap<String,Object>();
        Calendar refDate = Calendar.getInstance();
        map.put( "date", refDate.getTime() );
        //((GSONBuilderService)sc).setDateFormat( "MM/dd/yyyy" );
        sc.setDateFormat( MMddyyyy );
        String serJson = sc.ser( map );  
        System.out.println("serJson:"+ serJson);
        assertTrue("Serialize with Adapter failed ", serJson.matches( "\\{\"date\":\"\\d\\d/\\d\\d/\\d{4}\"\\}" ));
        Date date = JsonPath.parse(serJson).read("$.date", Date.class);// .using(conf)
        Calendar parsedDate = Calendar.getInstance();
        parsedDate.setTime(date);
        assertEquals("Serialization failed ", refDate.get(Calendar.DATE), parsedDate.get(Calendar.DATE));  
    }
    
    @Test
    public void testCollection() throws Exception
    {
      List<Rectangle> rectList = new ArrayList<Rectangle>();
      for ( int i = 0; i < 10; i++ )
      {
          Rectangle filteredRect = new Rectangle(i,i,"rect"+i);
          rectList.add( filteredRect ); 
      }
      String result = sc.ser( rectList );
      assertEquals("collect ser","[{'w':0,'h':0,'name':'rect0'},{'w':1,'h':1,'name':'rect1'},{'w':2,'h':2,'name':'rect2'},{'w':3,'h':3,'name':'rect3'},{'w':4,'h':4,'name':'rect4'},{'w':5,'h':5,'name':'rect5'},{'w':6,'h':6,'name':'rect6'},{'w':7,'h':7,'name':'rect7'},{'w':8,'h':8,'name':'rect8'},{'w':9,'h':9,'name':'rect9'}]",
              result.replace( '"', '\'' )       );
      List<Map<String, Object>> beanList2 = JsonPath.parse(result).read("$[-2:]", List.class);
      assertEquals("Expect 2 Elements failed ", 2, beanList2.size());     
     }
    @Test
    public void testSerializeTypeAdapterForCollection() throws Exception
    {
      sc.addAdapter( "Collection Adapter",  ArrayList.class , TypeAdapterForCollection.class );
      List<Rectangle> rectList = new ArrayList<Rectangle>();
      for ( int i = 0; i < 10; i++ )
      {
          Rectangle filteredRect = new Rectangle(i,i,"rect"+i);
          rectList.add( filteredRect ); 
      }
      String serColl = sc.ser( rectList  );
      assertEquals("collect ser","[{'rect0':0,'rect1':1,'rect2':4,'rect3':9,'rect4':16,'rect5':25,'rect6':36,'rect7':49,'rect8':64,'rect9':81}]",
              serColl.replace( '"', '\'' )       );
      
      System.out.println("serColl: "+ serColl);
      TypeRef<List<Rectangle>> typeRef = new TypeRef<List<Rectangle>>() { };
      List<Rectangle> result = JsonPath.parse(serColl).read("$",typeRef);
      System.out.println("result: "+ result);
      int idx = 0;
      for (Rectangle rect : result) {
          assertEquals("deser reread size failed", (idx * idx), rect.getSize());
          idx++;
      }
     }



}
