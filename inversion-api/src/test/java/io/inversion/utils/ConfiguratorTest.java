/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.utils;

import io.inversion.Api;
import io.inversion.Endpoint;
import io.inversion.Engine;
import io.inversion.action.misc.MockAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(Lifecycle.PER_METHOD)
public class ConfiguratorTest {
    @BeforeEach
    public void clearConfiguration() {
        Config.clearConfiguration();
    }

//   /**
//    * Test that properties files are loaded for the given
//    * profile in the correct order allowing keys to be
//    * overridden. 
//    */
//   @Test
//   public void profileOverrides()
//   {
//      Config.loadConfiguration("io/inversion/utils/profileOverrides", "dev");
//
//      Engine dev = new Engine();
//      dev.startup();
//
//      Api devApi = dev.getApi("northwind");
//      assertEquals("20", ((MockDb) devApi.getDb("db")).getProperty1());
//      assertEquals(0, devApi.getActions().size());
//
//      Config.loadConfiguration("io/inversion/utils/profileOverrides", "prod");
//      Engine prod = new Engine();
//      prod.startup();
//
//      Api prodApi = prod.getApi("northwind");
//
//      assertEquals("70", ((MockDb) prodApi.getDb("db")).getProperty1());
//      assertEquals(1, prodApi.getActions().size());
//      assertTrue(prodApi.getActions().get(0) instanceof AuthAction);
//   }

    @Test
    public void handWiredApiGetsConfigged() {
        MockAction mockAction = new MockAction().withName("mockAction");
        Endpoint   endpoint   = new Endpoint().withIncludeOn("*", "*").withName("myEndpoint").withAction(mockAction);

        Api api = new Api("myApi").withEndpoint(endpoint);

        Engine e = new Engine().withApi(api);
        e.withConfigPath("io/inversion/utils/handWiredApiGetsConfigged");
        e.startup();

        assertEquals("http://127.0.0.1/testvalue", mockAction.getJsonUrl());
    }


    @Test
    public void test_CodeWiredObjectsWithSameName_throwsException() {

        try{
            MockAction mockAction = new MockAction().withName("duplicateName");
            Endpoint   endpoint   = new Endpoint().withIncludeOn("*", "*").withName("myEndpoint").withAction(mockAction);
            Api api = new Api("duplicateName").withEndpoint(endpoint);
            Engine e = new Engine().withApi(api);
            e.startup();
        }
        catch(Exception ex){
            return;
        }
        fail();
    }

    @Test
    public void test_diObjectHasSameNameAsCodeWiredObject_throwsException() {

    }

        //
    //   /**
    //    * Test the stability of properties file encoding/decoding
    //    * by encoding an Api to a properties file, decoding it back
    //    * into an object model and then encoding it back into a
    //    * second properties file.  The two properties files should
    //    * match if the encoder/decoder worked propertly
    //    *  
    //    * @throws ApiException
    //    */
    //   @Test
    //   public void encodingDecodingEncoding_shouldNotChangeModel() throws Exception
    //   {
    //      Engine dev = new Engine();
    //      dev.getConfigurator().withProfile("dev");
    //      dev.getConfigurator().withConfigPath("io/inversion/utils");
    //      dev.startup();
    //      
    //      //encode
    //      Api source = dev.getApi("northwind");
    //      Properties props1 = Configurator.encode(source);
    //
    //      //decode
    //      Wirer w = new Wirer();
    //      w.load(props1);
    //
    //      //encode again
    //      Api copy1 = (Api) w.getBean("northwind");
    //      Properties props2 = Configurator.encode(copy1);
    //
    //      //props should match
    //      assertTrue(compare(props1, props2));
    //   }
    //
    //   //   @Test
    //   //   public void testEncodeDecodeEncodeAccuracy2() throws Exception
    //   //   {
    //   //      Api source = Demo001SqlDbNorthwind.buildApi();
    //   //      Properties props1 = Configurator.encode(source);
    //   //
    //   //      Wirer w = new Wirer();
    //   //      w.load(props1);
    //   //
    //   //      Api copy1 = (Api) w.getBean(source.getName());
    //   //      Properties props2 = Configurator.encode(copy1);
    //   //
    //   //      assertTrue(compare(props1, props2));
    //   //   }
    //
    //   public static boolean compare(Properties props1, Properties props2) throws IOException
    //   {
    //      return compare(print(props1), print(props2));
    //   }
    //
    //   public static boolean compare(String str1, String str2) throws IOException
    //   {
    //      String line1 = null;
    //      String line2 = null;
    //      BufferedReader r1 = new BufferedReader(new StringReader(str1));
    //      BufferedReader r2 = new BufferedReader(new StringReader(str2));
    //
    //      while (true)
    //      {
    //         line1 = r1.readLine();
    //         line2 = r2.readLine();
    //
    //         if (Utils.equal(line1, line2))
    //         {
    //            //               if (line1 != null)
    //            //                  System.out.println(line1);
    //         }
    //         else
    //         {
    //            System.out.println("DIFFERENT LINE1: " + line1);
    //            System.out.println("DIFFERENT LINE2: " + line2);
    //            return false;
    //         }
    //
    //         if (line1 == null || line2 == null)
    //            break;
    //      }
    //
    //      return true;
    //   }
    //
    //   public static String print(Properties props)
    //   {
    //      StringBuilder buff = new StringBuilder();
    //      List<String> keys = new ArrayList(props.keySet());
    //      Collections.sort(keys);
    //      keys.forEach(k -> {
    //         if (props.get(k) != null)
    //            buff.append(k).append("=").append("'").append(props.get(k)).append("'\r\n");
    //      });
    //      return buff.toString();
    //   }
}
