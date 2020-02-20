package io.inversion.cloud.model;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import io.inversion.cloud.model.Db;

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
      return getClass().getSimpleName().indexOf("IntegTest") > -1;
   }

}
