package io.inversion.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import io.inversion.Api;
import io.inversion.Engine;
import io.inversion.Response;
import io.inversion.action.db.DbAction;
import io.inversion.jdbc.JdbcDb;

public class JdbcConfiguratorTest {
   //TODO: put this back in
   //   @Test
   //   public void encodingDecodingEncoding_shouldNotChangeModel() throws Exception
   //   {
   //      Api api = new Api("northwind");
   //
   //      JdbcDb db = new JdbcDb("h2", //
   //                             "org.h2.Driver", //
   //                             "jdbc:h2:mem:JdbcConfiguratorTest;IGNORECASE=TRUE;DB_CLOSE_DELAY=-1", //
   //                             "sa", //
   //                             "", //
   //                             JdbcDb.class.getResource("northwind-h2.ddl").toString());
   //      
   //      db.getConnection();//this should bootstrap the ddl
   //      db.shutdown();
   //      
   //      
   //      //the wiring process causes ddl to run again which we don't want...so use a db without it
   //      db = new JdbcDb("h2", //
   //            "org.h2.Driver", //
   //            "jdbc:h2:mem:JdbcConfiguratorTest;IGNORECASE=TRUE;DB_CLOSE_DELAY=-1", //
   //            "sa", //
   //            "");
   //
   //      api.withDb(db);
   //      api.withEndpoint("*", "/*", new DbAction());
   //      api.startup();
   //      
   //
   //      //encode
   //      Properties props1 = Configurator.encode(api);
   //      
   //      //api.shutdown(); 
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
   //      assertTrue(ConfiguratorTest.compare(props1, props2));
   //
   //      Engine e = new Engine(copy1);
   //      e.withApi(api);
   //      e.startup();
   //
   //      Response resp = e.get("northwind/orders");
   //      assertEquals(25, resp.getData().length());
   //
   //   }
}
