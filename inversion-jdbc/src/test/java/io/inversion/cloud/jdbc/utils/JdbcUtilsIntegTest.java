/*
 * Copyright (c) 2015-2020 Rocket Partners, LLC
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
package io.inversion.cloud.jdbc.utils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import io.inversion.cloud.jdbc.JdbcDbApiFactory;
import io.inversion.cloud.jdbc.db.JdbcDb;
import io.inversion.cloud.model.Index;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.Rows;
import io.inversion.cloud.utils.Rows.Row;
import junit.framework.TestCase;

public class JdbcUtilsIntegTest extends TestCase
{
   @Test
   public void h2Upsert_oneNewRecordOneOldRecord_twoKeysReturned(Connection conn, String tableName, Index index, List<Map<String, Object>> rows) throws Exception
   {
      //TODO: implement me
   }

   @Test
   public void mysqlUpsert_oneNewRecordOneOldRecord_twoKeysReturned(Connection conn, String tableName, List<Map<String, Object>> rows) throws Exception
   {
      //TODO: implement me
   }

   @Test
   public void postgresUpsert_oneNewRecordOneOldRecord_twoKeysReturned(Connection conn, String tableName, Index index, List<Map<String, Object>> rows) throws Exception
   {
      //TODO: implement me
   }

   @Test
   public void sqlserverUpsert_oneNewRecordOneOldRecord_twoKeysReturned(Connection conn, String tableName, Index index, List<Map<String, Object>> rows) throws Exception
   {
      //TODO: implement me
   }

   //   @Test
   //   public void upsert_test() throws Exception
   //   {
   //      Engine engine = JdbcDbApiFactory.service(true, true);
   //      Response res = null;
   //
   //      JdbcDb mysql = (JdbcDb) engine.getApi("northwind").getDb("mysql");
   //
   //      if (mysql.isType("mysql"))
   //      {
   //         Connection conn = mysql.getConnection();
   //         try
   //         {
   //            Rows rows = JdbcUtils.selectRows(conn, "SELECT * FROM Orders WHERE OrderID in(10257, 10395, 10476, 10486)");
   //
   //            for (Row row : rows)
   //            {
   //               row.put("shipaddress", "testing_upsert");
   //            }
   //
   //            Map clone1 = new HashMap(rows.get(0));
   //            clone1.remove("OrderID");
   //
   //            Map clone2 = new HashMap(rows.get(0));
   //            clone2.put("OrderID", 1);
   //
   //            List<Map<String, Object>> toUpsert = new ArrayList(rows);
   //            toUpsert.add(clone1);
   //            toUpsert.add(clone2);
   //            List generatedKeys = JdbcUtils.upsert(conn, "Orders", mysql.getCollection("Orders").getPrimaryIndex().getColumnNames(), toUpsert);
   //
   //            //[10257, 10395, 10476, 10486, 222001, 1]
   //
   //            assertEquals("11078", generatedKeys.get(4));//should be next auto increment key
   //            assertEquals("1", generatedKeys.get(5));
   //         }
   //         finally
   //         {
   //            conn.close();
   //         }
   //
   //      }
   //
   //   }
}
