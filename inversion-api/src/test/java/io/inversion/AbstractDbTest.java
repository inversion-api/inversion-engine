package io.inversion;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import io.inversion.Db;

public interface AbstractDbTest
{
   public String getType();

   public Db getDb();

   public void setDb(Db db);

   @BeforeAll
   public default void beforeAll_initializeDb()
   {
      initializeDb();
   }

   @AfterAll
   public default void afterAll_finalizeDb()
   {
      finalizeDb();
   }

   public void initializeDb();

   public default void finalizeDb()
   {
      //System.out.println("AbstractDbTest.finalizeDb()");

      Db db = getDb();
      if (db != null)
         db.shutdown();

      setDb(null);
   }

   public default boolean isIntegTest()
   {
      return getClass().getSimpleName().indexOf("IntegTest") > -1 || (getClass().getSimpleName().indexOf("UnitTest") < 0 && getClass().getSimpleName().indexOf("H2") > -1);
   }

}
