package io.rocketpartners.cloud.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import io.rocketpartners.cloud.action.sql.SqlServiceFactory;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Configurator;
import io.rocketpartners.cloud.utils.Utils;
import io.rocketpartners.cloud.utils.Wirer;
import junit.framework.TestCase;

public class TestConfigurator extends TestCase
{
   //
   //   @Test
   //   public void testWire1() throws Exception
   //   {
   //      Properties props = new Properties();
   //      props.load(getClass().getResourceAsStream("wire1.properties"));
   //
   //      Wirer w = new Wirer();
   //      w.load(props);
   //
   //      Api api = w.getBean(Api.class);
   //      Collection coll = api.getCollection("locations");
   //      List<Relationship> rels = coll.getEntity().getRelationships();
   //      System.out.println(rels);
   //   }

      @Test
      public void testPropsConfig1() throws Exception
      {
         Service service = SqlServiceFactory.service();
         Api source = service.getApi("northwind");
   
         Properties props1 = Configurator.encode(source);
   
         List<String> keys = new ArrayList(props1.keySet());
         Collections.sort(keys);
   
         //      for (String key : keys)
         //      {
         //         String value = props1.getProperty(key);
         //
         //         if (key.indexOf(".api") >= 0)
         //            System.out.println(key + "=" + value);
         //      }
   
         Wirer w = new Wirer();
         w.load(props1);
   
         Api copy1 = (Api) w.getBean("northwind");
   
         Properties props2 = Configurator.encode(copy1);
   
         String print1 = print(props1);
         String print2 = print(props2);
   
         //      assertTrue(props1.containsKey("h2.pass"));
         //      assertTrue(props2.containsKey("h2.pass"));
   
         //compare(print1, print2);
         assertTrue(compare(print1, print2));
      }
   
      protected boolean compare(String str1, String str2) throws IOException
      {
         String line1 = null;
         String line2 = null;
         BufferedReader r1 = new BufferedReader(new StringReader(str1));
         BufferedReader r2 = new BufferedReader(new StringReader(str2));
   
         while (true)
         {
            line1 = r1.readLine();
            line2 = r2.readLine();
   
            if (Utils.equal(line1, line2))
            {
//               if (line1 != null)
//                  System.out.println(line1);
            }
            else
            {
               System.out.println("DIFFERENT LINE1: " + line1);
               System.out.println("DIFFERENT LINE2: " + line2);
               return false;
            }
   
            if (line1 == null || line2 == null)
               break;
         }
   
         return true;
      }
   
      protected String print(Properties props)
      {
         StringBuffer buff = new StringBuffer();
         List<String> keys = new ArrayList(props.keySet());
         Collections.sort(keys);
         keys.forEach(k -> {
            if (props.get(k) != null)
               buff.append(k).append("=").append("'").append(props.get(k)).append("'\r\n");
         });
         return buff.toString();
      }

}
