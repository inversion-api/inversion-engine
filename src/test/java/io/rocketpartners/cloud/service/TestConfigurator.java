package io.rocketpartners.cloud.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import io.rocketpartners.cloud.action.security.AuthAction;
import io.rocketpartners.cloud.action.sql.SqlDb;
import io.rocketpartners.cloud.action.sql.SqlServiceFactory;
import io.rocketpartners.cloud.demo.Demo001SqlDbNorthwind;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Relationship;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Configurator;
import io.rocketpartners.cloud.utils.Utils;
import io.rocketpartners.cloud.utils.Wirer;
import junit.framework.TestCase;

public class TestConfigurator extends TestCase
{

   /**
    * Test that properties files are loaded for the given
    * profile in the correct order allowing keys to be
    * overridden. 
    */
   @Test
   public void testConfigSimple()
   {
      Service dev = new Service();
      dev.setProfile("dev");
      dev.setConfigPath("io/rocketpartners/cloud/service/config/");
      dev.startup();

      Api devApi = dev.getApi("northwind");
      assertEquals(20, ((SqlDb) devApi.getDb("db")).getPoolMax());
      assertEquals(0, devApi.getActions().size());

      Service prod = new Service();
      prod.setProfile("prod");
      prod.setConfigPath("io/rocketpartners/cloud/service/config/");
      prod.startup();

      Api prodApi = prod.getApi("northwind");

      assertEquals(70, ((SqlDb) prodApi.getDb("db")).getPoolMax());
      assertEquals(1, prodApi.getActions().size());
      assertTrue(prodApi.getActions().get(0) instanceof AuthAction);
   }

   /**
    * Test the stability of properties file encoding/decoding
    * by encoding an Api to a properties file, decoding it back
    * into an object model and then encoding it back into a
    * second properties file.  The two properties files should
    * match if the encoder/decoder worked propertly
    *  
    * @throws Exception
    */
   @Test
   public void testEncodeDecodeEncodeAccuracy1() throws Exception
   {
      Service service = SqlServiceFactory.service();
      Api source = service.getApi("northwind");
      Properties props1 = Configurator.encode(source);

      Wirer w = new Wirer();
      w.load(props1);

      Api copy1 = (Api) w.getBean("northwind");
      Properties props2 = Configurator.encode(copy1);

      assertTrue(compare(props1, props2));
   }

   @Test
   public void testEncodeDecodeEncodeAccuracy2() throws Exception
   {
      Api source = Demo001SqlDbNorthwind.buildApi();
      Properties props1 = Configurator.encode(source);

      Wirer w = new Wirer();
      w.load(props1);

      Api copy1 = (Api) w.getBean(source.getName());
      Properties props2 = Configurator.encode(copy1);

      assertTrue(compare(props1, props2));
   }

   protected boolean compare(Properties props1, Properties props2) throws IOException
   {
      return compare(print(props1), print(props2));
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
