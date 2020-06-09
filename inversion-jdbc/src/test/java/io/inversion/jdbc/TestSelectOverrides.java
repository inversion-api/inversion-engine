package io.inversion.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import io.inversion.Action;
import io.inversion.Api;
import io.inversion.ApiException;
import io.inversion.Collection;
import io.inversion.Engine;
import io.inversion.Request;
import io.inversion.Response;
import io.inversion.action.db.DbAction;

public class TestSelectOverrides
{
   @Test
   public void queryWithoutCollection() throws Exception
   {
      JdbcDb db = JdbcDbFactory.bootstrapH2("queryWithoutCollection");
      db.withExcludeColumns("collection", "entity", "relationship");

      Engine engine = new Engine().withApi(new Api("northwind") //
                                                               .withEndpoint("*", db.getType() + "/*", new Action<Action>()
                                                                  {
                                                                     public void doGet(Request req, Response res) throws ApiException
                                                                     {
                                                                        String collectionKey = req.getCollectionKey();

                                                                        if ("CustomerOrders".equalsIgnoreCase(collectionKey))
                                                                        {
                                                                           req.getChain().put("CustomerOrders.select", "SELECT * FROM Customers c JOIN Orders o ON o.CustomerID = c.CustomerID");
                                                                        }
                                                                        else if ("customers".equalsIgnoreCase(collectionKey))
                                                                        {
                                                                           req.getChain().put(collectionKey + ".select", "SELECT * FROM Customers WHERE COUNTRY='USA'");
                                                                        }
                                                                        else if ("orders".equalsIgnoreCase(collectionKey))
                                                                        {
                                                                           Collection customers = req.getApi().getCollection("customers");
                                                                           req.withCollection(customers);
                                                                        }
                                                                     }

                                                                  }.withIncludeOn("*", "{collection:CustomerOrders|customers|orders}/*"), new DbAction())//
                                                               .withDb(db));

      Response res = null;

      res = engine.get("northwind/h2/CustomerOrders?COMPANYNAME=Ernst Handel");
      assertEquals(2, res.getFoundRows());
      assertNull(res.find("data.0.href"));
      res.dump();

      res = engine.get("northwind/h2/customers?contactname=Howard Snyder");
      assertEquals("http://localhost/northwind/h2/customers/GREAL", res.find("data.0.href"));
      res.dump();

      res = engine.get("northwind/h2/orders?contactname=Howard Snyder");
      assertEquals("http://localhost/northwind/h2/orders/GREAL", res.find("data.0.href"));
      res.dump();
   }
}
