package io.inversion.jdbc;

import io.inversion.AbstractDbTest;
import io.inversion.AbstractEngineTest;
import io.inversion.Chain;
import io.inversion.Db;
import io.inversion.jdbc.JdbcDb.JdbcConnectionLocal;

public interface AbstractJdbcDbEngineTest extends AbstractDbTest, AbstractEngineTest
{
   @Override
   public default void initializeDb()
   {
      Db db = getDb();
      if (db == null)
      {
         JdbcConnectionLocal.closeAll();
         Chain.resetAll();

         if (isIntegTest())
            db = JdbcDbFactory.buildDb(getType(), getClass().getSimpleName());
         else
            db = new JdbcDb(getType()).withType(getType());

         setDb(db);
      }

   }
}
