/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
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
package io.inversion.jdbc;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import io.inversion.Db;

class JdbcConnectionLocal {

   static Map<Db, Map<Thread, Connection>> dbToThreadMap = new Hashtable();
   static Map<Thread, Map<Db, Connection>> threadToDbMap = new Hashtable();

   public static void closeAll() {
      for (Thread thread : threadToDbMap.keySet()) {
         try {
            close(thread);
         }
         catch (Exception ex) {
            //ex.printStackTrace();
         }
      }

      //System.out.println(dbToThreadMap);
      //System.out.println(threadToDbMap);
   }

   public static Connection getConnection(Db db) {
      return getConnection(db, Thread.currentThread());
   }

   static Connection getConnection(Db db, Thread thread) {
      Map<Thread, Connection> threadToConnMap = dbToThreadMap.get(db);
      if (threadToConnMap == null)
         return null;

      return threadToConnMap.get(thread);
   }

   public static void putConnection(Db db, Connection connection) {
      putConnection(db, Thread.currentThread(), connection);
   }

   static void putConnection(Db db, Thread thread, Connection connection) {
      Map<Thread, Connection> threadToConnMap = dbToThreadMap.get(db);
      if (threadToConnMap == null) {
         threadToConnMap = new Hashtable();
         dbToThreadMap.put(db, threadToConnMap);
      }
      threadToConnMap.put(thread, connection);

      Map<Db, Connection> dbToConnMap = threadToDbMap.get(thread);
      if (dbToConnMap == null) {
         dbToConnMap = new Hashtable();
         threadToDbMap.put(thread, dbToConnMap);
      }
      dbToConnMap.put(db, connection);

   }

   public static void commit() throws Exception {
      Exception toThrow = null;

      Map<Db, Connection> dbToConnMap = threadToDbMap.get(Thread.currentThread());
      if (dbToConnMap != null) {
         java.util.Collection<Connection> connections = dbToConnMap.values();
         for (Connection conn : connections) {
            try {
               if (!(conn.isClosed() || conn.getAutoCommit())) {
                  conn.commit();
               }
            }
            catch (Exception ex) {
               if (toThrow == null)
                  toThrow = ex;
            }
         }
      }

      if (toThrow != null)
         throw toThrow;
   }

   public static void rollback() throws Exception {
      Exception toThrow = null;

      Map<Db, Connection> dbToConnMap = threadToDbMap.get(Thread.currentThread());
      if (dbToConnMap != null) {
         for (Connection conn : dbToConnMap.values()) {
            try {
               if (!(conn.isClosed() || conn.getAutoCommit())) {
                  conn.rollback();
               }
            }
            catch (Exception ex) {
               if (toThrow == null)
                  toThrow = ex;
            }
         }
      }

      if (toThrow != null)
         throw toThrow;
   }

   public static void close() throws Exception {
      close(Thread.currentThread());
   }

   static void close(Thread thread) throws Exception {
      Exception toThrow = null;

      Map<Db, Connection> dbToConnMap = threadToDbMap.remove(thread);

      if (dbToConnMap != null) {
         List<Db> dbs = new ArrayList(dbToConnMap.keySet());

         for (Db db : dbs)//Connection conn : dbToConnMap.values())
         {
            //--
            //-- cleanup the reverse mapping first
            Map<Thread, Connection> threadToConnMap = dbToThreadMap.get(db);
            threadToConnMap.remove(thread);

            if (threadToConnMap.size() == 0)
               dbToThreadMap.remove(db);
            //--
            //--

            try {
               Connection conn = dbToConnMap.get(db);
               if (!conn.isClosed()) {
                  conn.close();
               }
            }
            catch (Exception ex) {
               if (toThrow == null)
                  toThrow = ex;
            }
         }

         if (dbToConnMap.size() == 0)
            threadToDbMap.remove(thread);
      }

      if (toThrow != null)
         throw toThrow;
   }
}
