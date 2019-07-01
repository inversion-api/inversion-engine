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
import io.rocketpartners.cloud.service.Configurator;
import io.rocketpartners.cloud.service.Configurator.AutoWire;
import io.rocketpartners.cloud.utils.Utils;
import junit.framework.TestCase;

public class TestConfigurator extends TestCase
{
   @Test
   public void testPropsConfig1() throws Exception
   {
      Api source = SqlServiceFactory.service().getApi("northwind");

      Properties props1 = Configurator.encode(source);

      AutoWire w = new AutoWire();
      w.load(props1);

      Api copy1 = (Api) w.getBean("northwind");

      Properties props2 = Configurator.encode(copy1);

      String print1 = print(props1);
      String print2 = print(props2);

//      assertTrue(props1.containsKey("h2.pass"));
//      assertTrue(props2.containsKey("h2.pass"));

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

         if (line1 == null && line2 == null)
            break;

         if (Utils.equal(line1, line2))
         {
            System.out.println(line1);
         }
         else
         {
            System.out.println("DIFFERENT LINE1: " + line1);
            System.out.println("DIFFERENT LINE2: " + line2);
            return false;
         }
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
