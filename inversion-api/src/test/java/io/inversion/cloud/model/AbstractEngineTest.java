package io.inversion.cloud.model;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import io.inversion.cloud.action.rest.RestAction;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;

public interface AbstractEngineTest extends AbstractDbTest
{
   public Engine getEngine();

   public void setEngine(Engine engine);

   @BeforeAll
   public default void beforeAll_initializeEngine()
   {
      if (isIntegTest())
         initializeEngine();
   }

   @AfterAll
   public default void afterAll_finalizeEngine()
   {
      finalizeEngine();
   }

   public default void initializeEngine()
   {
      Engine engine = getEngine();
      if (engine == null)
      {
         Db db = getDb();
         if (db == null)
         {
            initializeDb();
            db = getDb();
         }

         //System.out.println("AbstractEngineTest.initializeEngine()");

         engine = new Engine().withApi(new Api("northwind") //
                                                           .withEndpoint("*", db.getType() + "/*", new RestAction())//
                                                           .withDb(getDb()));
         engine.startup();
      }
      setEngine(engine);
   }

   public default void finalizeEngine()
   {
      //System.out.println("AbstractEngineTest.finalizeEngine()");

      Engine engine = getEngine();
      if (engine != null)
         engine.shutdown();

      setEngine(null);

      Chain.resetAll();
   }
}