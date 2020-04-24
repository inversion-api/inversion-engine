package io.inversion.cloud.jdbc;

import io.inversion.cloud.jdbc.db.JdbcDb;
import io.inversion.cloud.jdbc.db.JdbcDb.ConnectionLocal;
import io.inversion.cloud.model.AbstractDbTest;
import io.inversion.cloud.model.AbstractEngineTest;
import io.inversion.cloud.model.Db;
import io.inversion.cloud.service.Chain;

public interface AbstractJdbcDbEngineTest extends AbstractDbTest, AbstractEngineTest
{
   @Override
   public default void initializeDb()
   {
      Db db = getDb();
      if (db == null)
      {
         ConnectionLocal.closeAll();
         Chain.resetAll();

         if (isIntegTest())
            db = JdbcDbFactory.buildDb(getType(), getClass().getSimpleName());
         else
            db = new JdbcDb(getType()).withType(getType());

         setDb(db);
      }

   }
}
