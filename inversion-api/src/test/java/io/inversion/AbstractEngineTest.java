package io.inversion;

import io.inversion.Api;
import io.inversion.Db;
import io.inversion.action.db.DbAction;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public interface AbstractEngineTest extends AbstractDbTest {

   public Engine getEngine();

   public void setEngine(Engine engine);

   @BeforeAll
   public default void beforeAll_initializeEngine() {
      if (isIntegTest())
         initializeEngine();
   }

   @AfterAll
   public default void afterAll_finalizeEngine() {
      finalizeEngine();
   }

   public default void initializeEngine() {
      Engine engine = getEngine();
      if (engine == null) {
         Db db = getDb();
         if (db == null) {
            initializeDb();
            db = getDb();
         }

         engine = buildEngine(db);
         engine.startup();
      }
      setEngine(engine);
   }

   public default Engine buildEngine(Db db) {
      Engine engine = new Engine().withApi(new Api("northwind") //
                                                               .withEndpoint("*", db.getType() + "/*", new DbAction())//
                                                               .withDb(db));

      return engine;
   }

   public default void finalizeEngine() {
      //System.out.println("AbstractEngineTest.finalizeEngine()");

      Engine engine = getEngine();
      if (engine != null)
         engine.shutdown();

      setEngine(null);

      Chain.resetAll();
   }
}
