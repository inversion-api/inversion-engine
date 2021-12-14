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
package io.inversion.config;

import io.inversion.*;
import io.inversion.action.misc.MockAction;
import io.inversion.action.security.AuthAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(Lifecycle.PER_METHOD)
public class ContextTest {
    @BeforeEach
    public void clearConfiguration() {
        Config.clearConfiguration();
    }

    @Test
    public void test_findFile(){
        String path = "io/inversion/config/codeWiredApiGetsConfigged/inversion1.properties";
        URL url = Config.findUrl(this, path);
        assertNotNull(url);
    }


    @Test
    public void test_defaultsNotApplied() {

        Engine e   = new Engine();
        Api    api = new Api();

        Collection orders1 = new Collection("orders")//
                .withProperty("id", "string", false)//
                .withProperty("blah1", "string")//
                .withIndex("primaryIndex", "primary", true, "id");


        Db reportsDb = new MockDb("reportsDb") {
            protected void configApi(Api api) {
                withCollection(orders1);
                super.startup(api);
            }
        };

        api.withDb(reportsDb);
        e.withApi(api);


        e.startup();
    }


    /**
     * Test that properties files are loaded for the given
     * profile in the correct order allowing keys to be
     * overridden.
     */
    @Test
    public void test_propWiredMultipleApis() {
        Config.loadConfiguration(this, "io/inversion/config/propWiredMultipleApis", "dev");

        Engine e = new Engine();
        e.startup();

        Api api1 = e.getApi("api1");
        assertNotNull(api1);
        Db db1 = api1.getDb("db1");
        assertNotNull(db1);
        Endpoint ep1 = api1.getEndpoint("ep1");
        assertNotNull(ep1);
        Action ac1 = ep1.getAction("ac1");
        assertNotNull(ac1);
        Action ac2 = ep1.getAction("ac2");
        assertNotNull(ac2);
        Action ac3 = api1.getAction("ac3");
        assertNotNull(ac3);
        Collection col1 = db1.getCollection("col1");
        assertNotNull(col1);
        Collection col2 = db1.getCollection("col2");
        assertNotNull(col2);
        Property prop1 = col1.getProperty("prop1");
        assertNotNull(prop1);
        Property prop2 = col1.getProperty("prop2");
        assertNotNull(prop2);
        Property prop3 = col2.getProperty("prop3");
        assertNotNull(prop3);
        Property prop4 = col2.getProperty("prop4");
        assertNotNull(prop4);
        Index idx1 = col1.getIndex("idx1");
        assertNotNull(idx1);
        Property idxProp1 = idx1.getProperty(0);
        assertEquals(idxProp1, prop1);


        Api apiA = e.getApi("apiA");
        assertNotNull(apiA);
        Db dbA = apiA.getDb("dbA");
        assertNotNull(dbA);
        Endpoint epA = apiA.getEndpoint("epA");
        assertNotNull(epA);
        Action acA = epA.getAction("acA");
        assertNotNull(acA);
        Action acB = epA.getAction("acB");
        assertNotNull(acB);
        Action acC = apiA.getAction("acC");
        assertNotNull(acC);
        Collection colA = dbA.getCollection("colA");
        assertNotNull(colA);
        Collection colB = dbA.getCollection("colB");
        assertNotNull(colB);
        Property propA = colA.getProperty("propA");
        assertNotNull(propA);
        Property propB = colA.getProperty("propB");
        assertNotNull(propB);
        Property propC = colB.getProperty("propC");
        assertNotNull(propC);
        Property propD = colB.getProperty("propD");
        assertNotNull(propD);
        Index idxA = colA.getIndex("idxA");
        assertNotNull(idxA);
        Property idxPropA = idxA.getProperty(0);
        assertEquals(idxPropA, propA);


    }


    /**
     * Test that properties files are loaded for the given
     * profile in the correct order allowing keys to be
     * overridden.
     */
    @Test
    public void test_profileOverrides() {
        Config.loadConfiguration(this, "io/inversion/config/profileOverrides", "dev");

        Engine dev = new Engine();
        dev.startup();

        Api devApi = dev.getApi("northwind");
        assertEquals("20", ((MockDb) devApi.getDb("db")).getProperty1());
        assertEquals(0, devApi.getActions().size());

        Config.loadConfiguration(this, "io/inversion/config/profileOverrides", "prod");
        Engine prod = new Engine();
        prod.startup();

        Api prodApi = prod.getApi("northwind");

        assertEquals("70", ((MockDb) prodApi.getDb("db")).getProperty1());
        assertEquals(1, prodApi.getActions().size());
        assertTrue(prodApi.getActions().get(0) instanceof AuthAction);
    }

    @Test
    public void test_overlappingCollectionNamesFromDifferentDbs_OK() {

        Collection orders1 = new Collection("orders")//
                .withProperty("id", "string", false)//
                .withProperty("blah1", "string")//
                .withIndex("primaryIndex", "primary", true, "id");


        Collection orders2 = new Collection("orders")//
                .withProperty("id", "string", false)//
                .withProperty("blah2", "string")//
                .withIndex("primaryIndex", "primary", true, "id");


        Db orderDb = new MockDb("ordersDb") {
            protected void configApi(Api api) {
                withCollection(orders1);
                super.startup(api);
            }
        };

        Db reportsDb = new MockDb("reportsDb") {
            protected void configApi(Api api) {
                withCollection(orders2);
                super.startup(api);
            }
        };
        Api api = new Api();
        api.withDbs(orderDb, reportsDb);

        Engine e = new Engine();
        e.withApi(api);
        e.startup();

        for (Collection coll : api.getCollections()) {
            assertEquals(1, coll.getIndexes().size());
        }
    }


    @Test
    public void test_propWiredInvalidName_Ignored() {
        Engine e = new Engine();
        e.withConfigPath("io/inversion/config/propWiredInvalidName");
        e.startup();
    }


    @Test
    public void test_codeWiredInvalidName_CausesError() {
        try {
            Api    api = new Api("myAp!i");
            Engine e   = new Engine().withApi(api);
            e.startup();
        } catch (Exception ex) {
            return;
        }
        fail("The '!' in the Api name should have caused the config to throw an exception");
    }

    @Test
    public void test_invalidNameInCollection_CausesError() {
        try {
            Collection orders = new Collection("orders")//
                    .withProperty("ty,pe", "string", false);

            Db orderDb = new MockDb("ordersDb") {
                protected void configApi(Api api) {
                    withCollection(orders);
                    super.startup(api);
                }
            };

            Api api = new Api("myAp!i");
            api.withDb(orderDb);
            Engine e = new Engine().withApi(api);
            e.startup();
        } catch (Exception ex) {
            return;
        }
        fail("The ',' in the collection property name should have caused the config to throw an exception");
    }


    @Test
    public void test_codeWiredApiGetsConfigged() {
        MockAction mockAction = new MockAction().withName("mockAction");
        Endpoint   endpoint   = new Endpoint().withName("myEndpoint").withAction(mockAction);

        Api api = new Api("myApi").withEndpoint(endpoint);

        Engine e = new Engine().withApi(api);
        e.withConfigPath("io/inversion/config/codeWiredApiGetsConfigged");
        e.startup();

        assertEquals("http://127.0.0.1/testvalue", mockAction.getJsonUrl());
    }


    @Test
    public void test_codeWiredObjectsWithSameName_throwsException() {

        try {
            MockAction mockAction = new MockAction().withName("duplicateName");
            Endpoint   endpoint   = new Endpoint().withName("myEndpoint").withAction(mockAction);
            Api        api        = new Api("duplicateName").withEndpoint(endpoint);
            Engine     e          = new Engine().withApi(api);
            e.startup();
        } catch (Exception ex) {
            return;
        }
        fail();
    }

    @Test
    public void test_propWiredObjectNameAlreadyExits_throwsException() {
        try {
            MockAction action = new MockAction().withName("action");
            Api        api    = new Api("api").withEndpoint(action);
            Engine     e      = new Engine().withApi(api);
            e.withConfigPath("io/inversion/config/propWiredObjectNameAlreadyExits");
            e.startup();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("---------");
            return;
        }
        fail();
    }

    @Test
    public void test_multipleAnonymousEndpoints_OK() {
        Api    api = new Api("api").withEndpoint().withEndpoint();
        Engine e   = new Engine().withApi(api);
        e.startup();
    }


//       /**
//        * Test the stability of properties file encoding/decoding
//        * by encoding an Api to a properties file, decoding it back
//        * into an object model and then encoding it back into a
//        * second properties file.  The two properties files should
//        * match if the encoder/decoder worked propertly
//        *
//        * @throws ApiException
//        */
//       @Test
//       public void encodingDecodingEncoding_shouldNotChangeModel() throws Exception
//       {
//          Engine dev = new Engine();
//          dev.getWirer().withProfile("dev");
//          dev.getWirer().withConfigPath("io/inversion/utils");
//          dev.startup();
//
//          //encode
//          Api source = dev.getApi("northwind");
//          Properties props1 = Wirer.encode(source);
//
//          //decode
//          Wirer w = new Wirer();
//          w.load(props1);
//
//          //encode again
//          Api copy1 = (Api) w.getBean("northwind");
//          Properties props2 = Wirer.encode(copy1);
//
//          //props should match
//          assertTrue(compare(props1, props2));
//       }
    //
    //   //   @Test
    //   //   public void testEncodeDecodeEncodeAccuracy2() throws Exception
    //   //   {
    //   //      Api source = Demo001SqlDbNorthwind.buildApi();
    //   //      Properties props1 = Wirer.encode(source);
    //   //
    //   //      Wirer w = new Wirer();
    //   //      w.load(props1);
    //   //
    //   //      Api copy1 = (Api) w.getBean(source.getName());
    //   //      Properties props2 = Wirer.encode(copy1);
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
