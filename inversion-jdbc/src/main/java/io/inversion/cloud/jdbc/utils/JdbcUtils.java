/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;

import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.utils.Rows;
import io.inversion.cloud.utils.Rows.Row;
import io.inversion.cloud.utils.Utils;

/**
 * TODO: http://www.symantec.com/connect/articles/detection-sql-injection-and-cross-site-scripting-attacks
 *
 */
public class JdbcUtils
{
   static final String[] ILLEGALS_REGX = new String[]{"insert", "update", "delete", "drop", "truncate", "exec"};

   static Pattern[]      ILLEGALS      = new Pattern[ILLEGALS_REGX.length];

   static
   {
      for (int i = 0; i < ILLEGALS_REGX.length; i++)
      {
         ILLEGALS[i] = Pattern.compile("\\W*" + ILLEGALS_REGX[i] + "\\W+", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
      }
   }

   static List<SqlListener> listeners = new ArrayList();

   public static String getDbType(Connection conn)
   {
      String connstr = conn.toString().toLowerCase();
      if (connstr.indexOf("mysql") > -1)
         return "mysql";
      if (connstr.indexOf("postgres") > -1)
         return "postgres";
      if (connstr.indexOf("h2") > -1)
         return "h2";
      if (connstr.indexOf("sqlserver") > -1)
         return "sqlserver";

      return "unknown";
   }

   public static char colQuote(Connection conn)
   {
      String connstr = conn.toString().toLowerCase();
      if (connstr.indexOf("mysql") > -1 || connstr.indexOf("h2") > -1)
         return '`';

      return '"';
   }

   public static String quoteCol(Connection conn, Object str)
   {
      char c = colQuote(conn);
      return c + str.toString() + c;
   }

   public static void addSqlListener(SqlListener listener)
   {
      if (!listeners.contains(listener))
         listeners.add(listener);
   }

   public static void removeSqlListener(SqlListener listener)
   {
      listeners.remove(listener);
   }

   public static interface SqlListener
   {
      public void onError(String method, String sql, Object args, Exception ex);

      public void beforeStmt(String method, String sql, Object args);

      public void afterStmt(String method, String sql, Object args, Exception ex, Object result);
   }

   public static void notifyBefore(String method, String sql, Object args)
   {
      for (SqlListener listener : listeners)
      {
         listener.beforeStmt(method, sql, args);
      }
   }

   public static void notifyError(String method, String sql, Object args, Exception ex)
   {
      for (SqlListener listener : listeners)
      {
         listener.onError(method, sql, args, ex);
      }
   }

   public static void notifyAfter(String method, String sql, Object args, Exception ex, Object result)
   {
      for (SqlListener listener : listeners)
      {
         listener.afterStmt(method, sql, args, ex, result);
      }
   }

   public static Object execute(Connection conn, String sql, Object... vals) throws Exception
   {
      if (vals != null && vals.length == 1 && vals[0] instanceof Collection)
         vals = ((Collection) vals[0]).toArray();

      notifyBefore("execute", sql, vals);

      Exception ex = null;
      Statement stmt = null;
      ResultSet rs = null;
      Object rtval = null;

      try
      {
         if (isSelect(sql))
         {
            if (vals != null && vals.length > 0)
            {
               stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
               for (int i = 0; vals != null && i < vals.length; i++)
               {
                  ((PreparedStatement) stmt).setObject(i + 1, vals[i]);
               }
               rs = ((PreparedStatement) stmt).executeQuery();
            }
            else
            {
               stmt = conn.createStatement();
               rs = stmt.executeQuery(sql);
            }
            if (rs.next())
            {
               rtval = rs.getObject(1);
               return rtval;
            }
         }
         else
         {
            if (vals != null && vals.length > 0)
            {
               stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
               for (int i = 0; vals != null && i < vals.length; i++)
               {
                  ((PreparedStatement) stmt).setObject(i + 1, vals[i]);
               }
               ((PreparedStatement) stmt).execute();
            }
            else
            {
               stmt = conn.createStatement();
               stmt.execute(sql, Statement.RETURN_GENERATED_KEYS);
            }

            if (isInsert(sql))
            {
               try
               {
                  rs = stmt.getGeneratedKeys();
                  if (rs.next())
                  {
                     rtval = rs.getObject(1);
                     return rtval;
                  }
               }
               catch (SQLFeatureNotSupportedException e)
               {
                  notifyError("execute", sql, vals, e);
                  //do nothing
               }
            }
            else if (isUpdate(sql))
            {
               try
               {
                  rtval = stmt.getUpdateCount();
                  return rtval;
               }
               catch (SQLFeatureNotSupportedException e)
               {
                  notifyError("execute", sql, vals, e);
                  //do nothing
               }
            }
         }
      }
      catch (Exception e)
      {
         notifyError("execute", sql, vals, e);
         ex = new Exception(e.getMessage() + " SQL=" + sql, Utils.getCause(e));
         throw ex;
      }
      finally
      {
         close(rs, stmt);
         notifyAfter("execute", sql, vals, ex, rtval);
      }

      return null;
   }

   /*
   +------------------------------------------------------------------------------+
   | SELECT UTILS
   +------------------------------------------------------------------------------+
    */

   public static boolean isSelect(String sql)
   {
      return sql != null && sql.toLowerCase().trim().startsWith("select ");
   }

   public static Rows selectRows(Connection conn, String sql, Object... vals) throws Exception
   {
      if (vals != null && vals.length == 1 && vals[0] instanceof List)
         vals = ((List) vals[0]).toArray();

      notifyBefore("selectRows", sql, vals);

      Exception ex = null;
      Statement stmt = null;
      ResultSet rs = null;
      Rows rows = null;

      try
      {
         if (vals != null && vals.length > 0)
         {
            stmt = conn.prepareStatement(sql);
            for (int i = 0; vals != null && i < vals.length; i++)
            {
               Object o = vals[i];
               ((PreparedStatement) stmt).setObject(i + 1, o);
            }
            rs = ((PreparedStatement) stmt).executeQuery();
         }
         else
         {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
         }

         rows = new Rows();

         ResultSetMetaData rsmd = rs.getMetaData();
         int cols = rsmd.getColumnCount();
         for (int i = 1; i <= cols; i++)
         {
            rows.addKey(rsmd.getColumnLabel(i));
         }

         while (rs.next())
         {
            rows.addRow();
            for (int i = 0; i < cols; i++)
            {
               Object o = null;
               try
               {
                  o = rs.getObject(i + 1);

                  if (o instanceof Clob)
                  {
                     Reader reader = ((Clob) o).getCharacterStream();
                     char[] arr = new char[8 * 1024];
                     StringBuilder buffer = new StringBuilder();
                     int numCharsRead;
                     while ((numCharsRead = reader.read(arr, 0, arr.length)) != -1)
                     {
                        buffer.append(arr, 0, numCharsRead);
                     }
                     reader.close();
                     o = buffer.toString();
                  }
               }
               catch (Exception e)
               {
                  notifyError("selectRows", sql, vals, e);
               }
               rows.put(o);
            }
         }
      }
      finally
      {
         close(stmt, rs);
         notifyAfter("selectRows", sql, vals, ex, rows);
      }
      return rows;
   }

   public static Row selectRow(Connection conn, String sql, Object... vals) throws Exception
   {
      Rows rows = selectRows(conn, sql, vals);
      if (rows.size() > 0)
         return rows.get(0);

      return null;
   }

   public static int selectInt(Connection conn, String sql, Object... vals) throws Exception
   {
      Object val = selectValue(conn, sql, vals);
      if (val == null)
         return -1;
      return Integer.parseInt(val + "");
   }

   public static Object selectValue(Connection conn, String sql, Object... vals) throws Exception
   {
      Row row = selectRow(conn, sql, vals);
      if (row != null)
      {
         return row.get(row.keySet().iterator().next());
      }
      return null;
   }

   public static List selectList(Connection conn, String sql, Object... vals) throws Exception
   {
      List objs = new ArrayList();

      Rows rows = selectRows(conn, sql, vals);
      Object key = null;
      for (Row row : rows)
      {
         if (key == null)
            key = row.keySet().iterator().next();

         objs.add(row.get(key));
      }
      return objs;
   }

   public static <T> T selectObject(Connection conn, String sql, Class<T> clazz, Object... vals) throws Exception
   {
      Row row = selectRow(conn, sql, vals);
      if (row != null)
      {
         Object o = clazz.newInstance();
         poplulate(o, row);
         return (T) o;
      }

      return null;
   }

   public static <T> T selectObject(Connection conn, String sql, T o, Object... vals) throws Exception
   {
      Row row = selectRow(conn, sql, vals);
      if (row != null)
      {
         poplulate(o, row);
      }

      return o;
   }

   public static List selectObjects(Connection conn, String sql, Class type, Object... vals) throws Exception
   {
      List objs = new ArrayList();
      Rows rows = selectRows(conn, sql, vals);
      for (Row row : rows)
      {
         Object o = type.newInstance();
         poplulate(o, row);
         objs.add(o);
      }
      return objs;
   }

   public static Object poplulate(Object o, Map<String, Object> row)
   {
      for (Field field : getFields(o.getClass()))
      {
         try
         {
            Object val = row.get(field.getName());
            if (val != null)
            {
               val = convert(val, field.getType());

               if (val != null && val instanceof Collection)
               {
                  Collection coll = (Collection) field.get(o);
                  coll.addAll((Collection) val);
               }
               else
               {
                  field.set(o, val);
               }
            }
         }
         catch (Exception ex)
         {
            //OK
         }
      }

      return o;
   }

   /*
   +------------------------------------------------------------------------------+
   | INSERT UTILS
   +------------------------------------------------------------------------------+
    */

   public static boolean isInsert(String sql)
   {
      String lc = sql.toLowerCase().trim();
      return lc.startsWith("insert ") || lc.startsWith("merge ");
   }

   public static String buildInsertSQL(Connection conn, String tableName, Object[] columnNameArray)
   {
      StringBuffer sql = new StringBuffer("INSERT INTO ");
      sql.append(quoteCol(conn, tableName)).append(" (");
      sql.append(getColumnStr(conn, columnNameArray)).append(") VALUES (");
      sql.append(getQuestionMarkStr(columnNameArray)).append(")");

      return sql.toString();
   }

   public static Object insertMap(Connection conn, String tableName, Map row) throws Exception
   {
      List keys = new ArrayList();
      List values = new ArrayList();
      for (Object key : row.keySet())
      {
         keys.add(key);
         values.add(row.get(key));
      }
      String sql = buildInsertSQL(conn, tableName, keys.toArray());
      return execute(conn, sql, values.toArray());
   }

   public static List insertMaps(Connection conn, String tableName, List maps) throws Exception
   {
      List<Map<String, Object>> rows = (List<Map<String, Object>>) maps;

      List returnKeys = new ArrayList();
      LinkedHashSet keySet = new LinkedHashSet();

      for (Map row : rows)
      {
         keySet.addAll(row.keySet());
      }

      List<String> keys = new ArrayList(keySet);
      String sql = buildInsertSQL(conn, tableName, keys.toArray());

      Exception ex = null;
      PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      try
      {
         notifyBefore("insertMaps", sql, rows);

         for (Map row : rows)
         {
            for (int i = 0; i < keys.size(); i++)
            {
               Object value = row.get(keys.get(i));
               ((PreparedStatement) stmt).setObject(i + 1, value);
            }
            stmt.addBatch();
         }
         stmt.executeBatch();
         ResultSet rs = stmt.getGeneratedKeys();
         while (rs.next())
         {
            returnKeys.add(rs.getObject(1));
         }
      }
      catch (Exception e)
      {
         ex = e;
         notifyError("insertMaps", sql, rows, ex);
      }
      finally
      {
         JdbcUtils.close(stmt);
         notifyAfter("insertMap", sql, rows, ex, null);
      }
      return returnKeys;
   }

   public static void insert(Connection conn, Object o) throws Exception
   {
      insert(conn, o.getClass().getSimpleName(), o);
   }

   public static void insert(Connection conn, String table, Object o) throws Exception
   {
      Exception ex = null;
      PreparedStatement stmt = null;
      StringBuffer sql = null;
      try
      {
         sql = new StringBuffer("INSERT INTO ").append(table);

         StringBuffer namesClause = new StringBuffer(" (");
         StringBuffer valuesClause = new StringBuffer(") VALUES (");

         List values = new ArrayList();
         List<Field> fields = getFields(o.getClass());
         Field idField = null;
         for (int i = 0; i < fields.size(); i++)
         {
            Field field = fields.get(i);

            String name = field.getName();
            Object value = field.get(o);

            if (name.toLowerCase().equals("id") && (value == null || Long.parseLong(value + "") <= 0))
            {
               idField = field;
               continue;
            }

            //if (value != null)
            {
               values.add(value);
               namesClause.append(quoteCol(conn, name)).append(",");
               valuesClause.append("?,");
            }
         }

         sql.append(namesClause.substring(0, namesClause.length() - 1));
         sql.append(valuesClause.substring(0, valuesClause.length() - 1));
         sql.append(")");

         notifyBefore("insert", sql.toString(), o);

         if (idField == null)
         {
            stmt = conn.prepareStatement(sql.toString());
            for (int i = 0; i < values.size(); i++)
            {
               stmt.setObject(i + 1, values.get(i));
            }
            stmt.execute();
         }
         else
         {
            stmt = conn.prepareStatement(sql.toString(), new String[]{idField.getName()});
            for (int i = 0; i < values.size(); i++)
            {
               stmt.setObject(i + 1, values.get(i));
            }
            stmt.execute();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next())
            {
               Object id = rs.getLong(1);
               id = convert(id, idField.getType());
               idField.set(o, id);
            }
         }
      }
      catch (Exception e)
      {
         ex = e;
         notifyError("insertMap", sql.toString(), o, ex);
         throw ex;
      }
      finally
      {
         close(stmt);
         notifyAfter("insertMap", sql.toString(), o, ex, null);
      }

   }

   /*
   +------------------------------------------------------------------------------+
   | UPDATE UTILS
   +------------------------------------------------------------------------------+
    */

   public static String buildUpdateSQL(Connection conn, String tableName, Object[] setColumnNameArray, Object[] whereColumnNames)
   {
      // UPDATE tmtuple SET model_id = ? , subj = ? , pred = ? , obj = ? , declared = ?

      StringBuffer sql = new StringBuffer("UPDATE ");
      sql.append(quoteCol(conn, tableName)).append(" SET ");
      sql.append(getWhereColumnStr(conn, setColumnNameArray, ","));
      if (whereColumnNames != null && whereColumnNames.length > 0)
      {
         sql.append(" WHERE " + getWhereColumnStr(conn, whereColumnNames, ","));
      }
      return sql.toString();
   }

   public static boolean isUpdate(String sql)
   {
      return sql.toLowerCase().trim().startsWith("update ");
   }

   public static int updateRow(Connection conn, String tableName, String keyCol, String keyVal, Map row) throws Exception
   {
      List colNames = new ArrayList();
      List colValues = new ArrayList();
      for (Object key : row.keySet())
      {
         if (!keyCol.equalsIgnoreCase(key + ""))
         {
            colNames.add(key);
            colValues.add(row.get(key));
         }
      }
      colValues.add(keyVal);

      String sql = buildUpdateSQL(conn, tableName, colNames.toArray(), new String[]{keyCol});

      return (Integer) execute(conn, sql, colValues.toArray());
   }

   public static void update(Connection conn, Object o) throws Exception
   {
      update(conn, o.getClass().getSimpleName(), o);
   }

   public static void update(Connection conn, String tableName, Object o) throws Exception
   {
      Exception ex = null;
      PreparedStatement stmt = null;
      StringBuffer sql = null;
      try
      {
         sql = new StringBuffer("UPDATE ").append(quoteCol(conn, tableName)).append(" SET ");

         Object id = null;

         List values = new ArrayList();
         List<Field> fields = getFields(o.getClass());
         for (int i = 0; i < fields.size(); i++)
         {
            Field f = fields.get(i);
            if (f.getName().equalsIgnoreCase("id"))
            {
               id = f.get(o);
            }
            else
            {
               values.add(f.get(o));
               sql.append(quoteCol(conn, f.getName())).append(" = ?");
               if (i < fields.size() - 1)
               {
                  sql.append(',');
               }
            }
         }
         sql.append(" WHERE id = ?");

         notifyBefore("update", sql.toString(), values);

         stmt = conn.prepareStatement(sql.toString());
         for (int i = 0; i < values.size(); i++)
         {
            stmt.setObject(i + 1, values.get(i));
         }
         stmt.setObject(values.size() + 1, id);
         stmt.execute();
      }
      catch (Exception e)
      {
         ex = e;
         notifyError("update", sql.toString(), o, ex);
         throw ex;
      }
      finally
      {
         close(stmt);
         notifyAfter("update", sql.toString(), o, ex, null);
      }

   }

   /*
   +------------------------------------------------------------------------------+
   | UPSERT UTILS
   +------------------------------------------------------------------------------+
    */

   /**
    * Batches <code>rows</code> into groups containing identical keys and then 
    * inserts rows that are missing indexCols key values or attempts an upsert
    * for rows that have the key values...the row could have the key but still 
    * not exist in the db in cases where the key is not an autoincrement number.
    * 
    * @param conn
    * @param tableName
    * @param indexCols
    * @param rows
    * @return
    * @throws Exception
    */
   public static List upsert(Connection conn, String tableName, List<String> indexCols, List<Map<String, Object>> rows) throws Exception
   {
      List returnKeys = new ArrayList();
      if (rows.isEmpty())
         return returnKeys;

      List<Map<String, Object>> inserts = new ArrayList();
      List<Map<String, Object>> upserts = new ArrayList();

      for (Map row : rows)
      {
         boolean hasKeys = true;

         for (String indexCol : indexCols)
         {
            if (Utils.empty(row.get(indexCol)))
            {
               hasKeys = false;
               break;
            }
         }

         if (!hasKeys)
            inserts.add(row);
         else
            upserts.add(row);
      }

      //-- first insert rows that do not have values for the provided index column.
      //-- there is no way it could be an update without a key
      Set cols = null;
      List<Map<String, Object>> batch = new ArrayList();
      for (Map row : inserts)
      {
         if (cols == null)
         {
            cols = row.keySet();
         }
         else if (CollectionUtils.disjunction(cols, row.keySet()).size() > 0)
         {
            cols = row.keySet();
            returnKeys.addAll(insertMaps(conn, tableName, batch));
            batch.clear();
         }

         batch.add(row);
      }
      if (batch.size() > 0)
      {
         returnKeys.addAll(insertMaps(conn, tableName, batch));
         batch.clear();
      }

      //-- you can only batch upsert rows together if they share the same key set 
      //-- otherwise you may end up unintentionally nulling out cols from some rows
      cols = null;
      batch.clear();
      for (Map row : upserts)
      {
         if (cols == null)
         {
            cols = row.keySet();
         }
         else if (CollectionUtils.disjunction(cols, row.keySet()).size() > 0)
         {
            cols = row.keySet();
            returnKeys.addAll(upsertBatch(conn, tableName, indexCols, batch));
            batch.clear();
         }

         batch.add(row);
      }
      if (batch.size() > 0)
      {
         returnKeys.addAll(upsertBatch(conn, tableName, indexCols, batch));
         batch.clear();
      }

      return returnKeys;
   }

   static List upsertBatch(Connection conn, String tableName, List<String> idxCols, List<Map<String, Object>> rows) throws Exception
   {
      String type = getDbType(conn);
      List returnKeys = null;

      switch (type)
      {
         case "mysql":
            returnKeys = mysqlUpsertBatch(conn, tableName, idxCols, rows);
            break;

         case "postgres":
            returnKeys = postgresUpsertBatch(conn, tableName, idxCols, rows);
            break;

         case "sqlserver":
            returnKeys = sqlserverUpsertBatch(conn, tableName, idxCols, rows);
            break;

         default :
            returnKeys = h2UpsertBatch(conn, tableName, idxCols, rows);
            break;
      }

      if (returnKeys.size() != rows.size())
         throw new ApiException("Return key size does not equal supplied row size.");

      return returnKeys;
   }

   static List h2UpsertBatch(Connection conn, String tableName, List<String> idxCols, List<Map<String, Object>> rows) throws Exception
   {
      List returnKeys = new ArrayList();
      for (Map row : rows)
      {
         returnKeys.add(h2UpsertBatch(conn, tableName, idxCols, row));
      }
      return returnKeys;
   }

   static Object h2UpsertBatch(Connection conn, String tableName, List<String> idxCols, Map<String, Object> row) throws Exception
   {
      String sql = "";

      List cols = new ArrayList();
      List vals = new ArrayList();
      for (String col : row.keySet())
      {
         cols.add(col);
         vals.add(row.get(col));
      }

      String keyCols = "";
      for (int i = 0; i < idxCols.size(); i++)
      {
         keyCols += quoteCol(conn, idxCols.get(i));
         if (i < idxCols.size() - 1)
            keyCols += ", ";
      }

      sql += " MERGE INTO " + quoteCol(conn, tableName) + " (" + JdbcUtils.getColumnStr(conn, cols) + ")  KEY(" + keyCols + ") VALUES (" + getQuestionMarkStr(vals.size()) + ")";

      Exception ex = null;
      try
      {
         notifyBefore("upsert", sql, row);
         return execute(conn, sql, vals);
      }
      catch (Exception e)
      {
         ex = e;
         notifyError("upsert", sql, row, ex);
         throw e;
      }
      finally
      {
         notifyAfter("upsert", sql, row, ex, null);
      }
   }

   static List mysqlUpsertBatch(Connection conn, String tableName, List<String> idxCols, List<Map<String, Object>> rows) throws Exception
   {
      LinkedHashSet keySet = new LinkedHashSet();
      List returnKeys = new ArrayList();

      for (Map row : rows)
      {
         keySet.addAll(row.keySet());
      }

      List<String> keys = new ArrayList(keySet);
      String sql = mysqlBuildInsertOnDuplicateKeySQL(conn, tableName, keys.toArray());

      Exception ex = null;
      PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
      try
      {
         notifyBefore("upsert", sql, rows);

         for (Map<String, Object> row : rows)
         {
            for (int i = 0; i < keys.size(); i++)
            {
               Object value = row.get(keys.get(i));
               ((PreparedStatement) stmt).setObject(i + 1, value);
            }
            stmt.addBatch();
         }
         stmt.executeBatch();
      }
      catch (Exception e)
      {
         System.out.println(sql);
         e.printStackTrace();
         ex = e;
         notifyError("upsert", sql, rows, ex);
         throw e;
      }
      finally
      {
         ResultSet rs = stmt.getGeneratedKeys();
         boolean hadNext = false;
         while (rs.next())
         {
            //this was an autogenerated key
            hadNext = true;
            String key = rs.getString(1);
            returnKeys.add(key);
         }
         if (!hadNext)
         {
            //this was not an autogenerated key
            for (Map row : rows)
            {
               Object key = row.get(idxCols.get(0));
               if (key == null)
                  throw new RuntimeException("Unable to determine key value for upserted row.");

               returnKeys.add(key);
            }
         }
         JdbcUtils.close(stmt);
         notifyAfter("upsert", sql, rows, ex, null);
      }
      return returnKeys;
   }

   static String mysqlBuildInsertOnDuplicateKeySQL(Connection conn, String tableName, Object[] columnNameArray)
   {
      StringBuffer sql = new StringBuffer(buildInsertSQL(conn, tableName, columnNameArray));
      sql.append(" ON DUPLICATE KEY UPDATE ");
      for (int i = 0; i < columnNameArray.length; i++)
      {
         Object col = columnNameArray[i];
         sql.append("\r\n`").append(col).append("`= values(`").append(col).append("`)");
         if (i < columnNameArray.length - 1)
            sql.append(", ");
      }
      return sql.toString();
   }

   /**
    * https://stackoverflow.com/questions/17267417/how-to-upsert-merge-insert-on-duplicate-update-in-postgresql
    * 
    * @param conn
    * @param tableName
    * @param rows
    * @return
    * @throws Exception
    */
   static List postgresUpsertBatch(Connection conn, String tableName, List<String> idxCols, List<Map<String, Object>> rows) throws Exception
   {
      List returnKeys = new ArrayList();

      List<String> cols = new ArrayList(rows.get(0).keySet());

      StringBuffer buff = new StringBuffer(buildInsertSQL(conn, tableName, cols.toArray()));
      buff.append("\r\n ON CONFLICT (");
      for (int i = 0; i < idxCols.size(); i++)
      {
         buff.append(quoteCol(conn, idxCols.get(i)));
         if (i < idxCols.size() - 1)
            buff.append(", ");
      }
      buff.append(") DO UPDATE SET ");
      for (int i = 0; i < cols.size(); i++)
      {
         buff.append("\r\n ").append(quoteCol(conn, cols.get(i))).append(" = EXCLUDED." + quoteCol(conn, cols.get(i)));
         if (i < cols.size() - 1)
            buff.append(", ");
      }

      Exception ex = null;
      String sql = buff.toString();
      PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
      try
      {
         notifyBefore("upsert", sql, rows);

         for (Map<String, Object> row : rows)
         {
            for (int i = 0; i < cols.size(); i++)
            {
               Object value = row.get(cols.get(i));
               ((PreparedStatement) stmt).setObject(i + 1, value);
            }
            stmt.addBatch();
         }
         stmt.executeBatch();
      }
      catch (Exception e)
      {
         System.out.println(sql);
         ex = e;
         notifyError("upsert", sql, rows, ex);
         throw e;
      }
      finally
      {
         ResultSet rs = stmt.getGeneratedKeys();
         while (rs.next())
         {

            String key = rs.getString(1);
            returnKeys.add(key);
         }
         JdbcUtils.close(stmt);
         notifyAfter("upsert", sql, rows, ex, null);
      }
      return returnKeys;
   }

   /**
    * https://stackoverflow.com/questions/108403/solutions-for-insert-or-update-on-sql-server
    * @param sql
    * @return
    */
   static List sqlserverUpsertBatch(Connection conn, String tableName, List<String> idxCols, List<Map<String, Object>> rows) throws Exception
   {
      List returnKeys = new ArrayList();
      for (Map row : rows)
      {
         returnKeys.add(sqlserverUpsertBatch(conn, tableName, idxCols, row));
      }
      return returnKeys;
   }

   static Object sqlserverUpsertBatch(Connection conn, String tableName, List<String> idxCols, Map<String, Object> row) throws Exception
   {
      String returnKey = null;

      boolean hasIdx = true;
      for (String col : idxCols)
      {
         if (Utils.empty(row.get(col)))
         {
            hasIdx = false;
            break;
         }
      }

      Object[] keys = idxCols.toArray();
      Object[] cols = row.keySet().toArray();

      String sql = buildInsertSQL(conn, tableName, cols);
      if (hasIdx)
      {
         sql = buildUpdateSQL(conn, tableName, cols, keys) + "\r\n IF @@ROWCOUNT = 0 \r\n " + sql;
      }

      Exception ex = null;

      PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
      try
      {
         notifyBefore("upsert", sql, row);

         int colNum = 1;

         for (Object col : cols)
         {
            Object value = row.get(col);
            ((PreparedStatement) stmt).setObject(colNum++, value);
         }

         if (hasIdx)
         {
            for (Object key : keys)
            {
               Object value = row.get(key);
               ((PreparedStatement) stmt).setObject(colNum++, value);
            }

            for (Object col : cols)
            {
               Object value = row.get(col);
               ((PreparedStatement) stmt).setObject(colNum++, value);
            }
         }
         stmt.execute();
      }
      catch (Exception e)
      {
         ex = e;
         notifyError("upsert", sql, row, ex);
         throw e;
      }
      finally
      {
         ResultSet rs = stmt.getGeneratedKeys();
         if (rs.next())
         {

            returnKey = rs.getString(1);
         }
         JdbcUtils.close(stmt);
         notifyAfter("upsert", sql, row, ex, null);
      }
      return returnKey;
   }

   /*
   +------------------------------------------------------------------------------+
   | DELETE UTILS
   +------------------------------------------------------------------------------+
    */

   public boolean isDelete(String sql)
   {
      return sql.toLowerCase().trim().startsWith("delete ");
   }

   public static int deleteRow(Connection conn, String table, String keyCol, Object keyVal) throws Exception
   {
      String sql = "";
      sql += " DELETE FROM " + quoteCol(conn, table);
      sql += " WHERE " + keyCol + " = ?";
      Integer deletes = (Integer) execute(conn, sql, keyVal);
      if (deletes == null)
         deletes = -1;
      return deletes;

   }

   public static int deleteRows(Connection conn, String table, String keyCol, Object... keyVals) throws Exception
   {
      if (keyVals != null && keyVals.length == 1 && Collection.class.isAssignableFrom(keyVals[0].getClass()))
      {
         keyVals = ((Collection) keyVals[0]).toArray();
      }

      String sql = "";
      sql += " DELETE FROM " + quoteCol(conn, table);
      sql += " WHERE " + keyCol + " in (" + getQuestionMarkStr(keyVals.length) + ")";
      Integer deletes = (Integer) execute(conn, sql, keyVals);
      if (deletes == null)
         deletes = -1;
      return deletes;

   }

   public static void delete(Connection conn, Object o) throws Exception
   {
      delete(conn, o.getClass().getSimpleName(), o);
   }

   public static void delete(Connection conn, String tableName, Object o) throws Exception
   {
      Exception ex = null;
      PreparedStatement stmt = null;
      StringBuffer sql = null;
      try
      {
         sql = new StringBuffer("DELETE FROM ").append(quoteCol(conn, tableName));

         Object id = null;

         List<Field> fields = getFields(o.getClass());
         for (int i = 0; i < fields.size(); i++)
         {
            Field f = fields.get(i);
            if (f.getName().equalsIgnoreCase("id"))
            {
               id = f.get(o);
               break;
            }
         }
         sql.append(" WHERE id = ?");

         notifyBefore("delete", sql.toString(), o);

         stmt = conn.prepareStatement(sql.toString());
         stmt.setObject(1, id);
         stmt.execute();
      }
      catch (Exception e)
      {
         ex = e;
         notifyError("delete", sql.toString(), o, ex);
      }
      finally
      {
         close(stmt);
         notifyAfter("delete", sql.toString(), o, ex, null);
      }
   }

   /*
   +------------------------------------------------------------------------------+
   | DDL UTILS
   +------------------------------------------------------------------------------+
    */

   public static void runDdl(Connection conn, InputStream ddlStream) throws Exception
   {
      List<String> script = readDdl(ddlStream);
      runDdl(conn, script.toArray(new String[script.size()]));
   }

   public static List<String> readDdl(String string) throws IOException
   {
      return readDdl(new ByteArrayInputStream(string.getBytes()));
   }

   public static List<String> readDdl(InputStream ddlStream) throws IOException
   {
      BufferedReader br = new BufferedReader(new InputStreamReader(ddlStream));
      String line = null;
      String curLine = "";
      List<String> ddlList = new ArrayList<String>();
      int num = 0;
      while ((line = br.readLine()) != null)
      {
         num += 1;
         line = line.trim();

         if (line.length() == 0 || line.startsWith("--") || line.startsWith("#"))
            continue;

         curLine = curLine + "\r\n" + line;
         if (line.trim().endsWith(";"))
         {
            ddlList.add(curLine.trim());
            curLine = "";
         }
      }

      return ddlList;
   }

   public static void runDdl(Connection con, List<String> sql) throws SQLException
   {
      runDdl(con, sql.toArray(new String[sql.size()]));
   }

   public static void runDdl(Connection con, String[] sql) throws SQLException
   {
      //System.out.print("running ddl: ");

      if (sql != null && sql.length > 0)
      {
         boolean oldAutoCommit = con.getAutoCommit();
         con.setAutoCommit(false);
         try
         {
            Statement stmt = con.createStatement();
            try
            {
               for (int i = 0; i < sql.length; i++)
               {
                  try
                  {
                     stmt.execute(sql[i]);
                  }
                  catch (SQLException ex)
                  {
                     System.err.println("Error trying to run sql statement: \r\n" + sql[i] + "\r\n\r\n");
                     ex.printStackTrace();
                     throw ex;
                  }
               }
            }
            finally
            {
               stmt.close();
            }
            con.commit();
         }
         finally
         {
            con.setAutoCommit(oldAutoCommit);
         }
      }
      //System.out.println(".done");
   }

   /*
   +------------------------------------------------------------------------------+
   | MISC UTILS
   +------------------------------------------------------------------------------+
    */

   public static String getWhereColumnStr(Connection conn, Object[] columnNameArray, String sep)
   {
      StringBuffer sb = new StringBuffer();

      for (int i = 0; i < columnNameArray.length; i++)
      {
         sb.append(quoteCol(conn, columnNameArray[i]));
         sb.append(" = ? ");
         if (i < columnNameArray.length - 1)
         {
            sb.append(sep).append(" ");
         }
      }

      return sb.toString();
   }

   public static String getColumnStr(Connection conn, Object[] columnNameArray)
   {
      StringBuffer sb = new StringBuffer();

      for (int i = 0; i < columnNameArray.length; i++)
      {
         sb.append(quoteCol(conn, columnNameArray[i]));
         if (i < columnNameArray.length - 1)
         {
            sb.append(", ");
         }
      }

      return sb.toString();
   }

   public static String getInClauseStr(Collection vals)
   {
      StringBuffer sb = new StringBuffer();

      int i = 0;
      for (Object val : vals)
      {
         sb.append(val);
         if (i < vals.size() - 1)
         {
            sb.append(", ");
         }

         i++;
      }

      return sb.toString();
   }

   public static String getQuotedStr(Collection vals, String quote)
   {
      StringBuffer sb = new StringBuffer();

      int i = 0;
      for (Object val : vals)
      {
         sb.append(quote).append(val).append(quote);
         if (i < vals.size() - 1)
         {
            sb.append(", ");
         }

         i++;
      }

      return sb.toString();
   }

   public static String getQuotedInClauseStr(Collection vals)
   {
      StringBuffer sb = new StringBuffer();

      int i = 0;
      for (Object val : vals)
      {
         sb.append('"').append(val).append('"');
         if (i < vals.size() - 1)
         {
            sb.append(", ");
         }

         i++;
      }

      return sb.toString();
   }

   public static String getColumnStr(Connection conn, List columnNameArray)
   {
      StringBuffer sb = new StringBuffer();

      for (int i = 0; i < columnNameArray.size(); i++)
      {
         sb.append(quoteCol(conn, columnNameArray.get(i)));
         if (i < columnNameArray.size() - 1)
         {
            sb.append(", ");
         }
      }

      return sb.toString();
   }

   public static String getQuestionMarkStr(Object[] columnNameArray)
   {
      return getQuestionMarkStr(columnNameArray.length);
   }

   public static String getQuestionMarkStr(int numQMarks)
   {
      StringBuffer sb = new StringBuffer();

      for (int i = 0; i < numQMarks; i++)
      {
         sb.append("?");
         if (i < numQMarks - 1)
         {
            sb.append(",");
         }
      }

      return sb.toString();
   }

   public static String check(Object sql)
   {
      if (sql == null)
         return null;

      String str = sql.toString();

      for (int i = 0; i < ILLEGALS.length; i++)
      {
         Matcher m = ILLEGALS[i].matcher(str);
         if (m.find())
            throw new RuntimeException("Sql injection attack blocker on keyword \"" + ILLEGALS_REGX[i].trim() + "\".  You have modifying sql in a select statement: " + str);
      }
      return str;
   }

   public static void close(Object... toClose)
   {
      for (Object o : toClose)
      {
         try
         {
            if (o instanceof Connection)
               ((Connection) o).close();
            else if (o instanceof Statement)
               ((Statement) o).close();
            else if (o instanceof ResultSet)
               ((ResultSet) o).close();
         }
         catch (Exception ex)
         {
            //ex.printStackTrace();
         }
      }
   }

   public static <T> T convert(Object value, Class<T> type)
   {
      if (type.isAssignableFrom(value.getClass()))
      {
         return (T) value;
      }

      if (type.equals(boolean.class) || type.equals(Boolean.class))
      {
         if (Number.class.isAssignableFrom(value.getClass()))
         {
            long num = Long.parseLong(value + "");
            if (num <= 0)
               return (T) Boolean.FALSE;
            else
               return (T) Boolean.TRUE;
         }
         if (value instanceof Boolean)
            return (T) value;
      }
      if (value instanceof Number)
      {
         if (type.equals(Long.class) || type.equals(long.class))
         {
            value = ((Number) value).longValue();
            return (T) value;
         }
         else if (type.equals(Integer.class) || type.equals(int.class))
         {
            value = ((Number) value).intValue();
            return (T) value;
         }
         else if (type.isAssignableFrom(long.class))
         {
            value = ((Number) value).longValue();
            return (T) value;
         }
      }

      if (value == null)
         return null;

      String str = value + "";

      if (String.class.isAssignableFrom(type))
      {
         return (T) str;
      }
      else if (boolean.class.isAssignableFrom(type))
      {
         str = str.toLowerCase();
         return (T) (Boolean) (str.equals("true") || str.equals("t") || str.equals("1"));
      }
      else if (int.class.isAssignableFrom(type))
      {
         return (T) (Integer) Integer.parseInt(str);
      }
      else if (long.class.isAssignableFrom(type))
      {
         return (T) (Long) Long.parseLong(str);
      }
      else if (float.class.isAssignableFrom(type))
      {
         return (T) (Float) Float.parseFloat(str);
      }
      else if (Collection.class.isAssignableFrom(type))
      {
         Collection list = new ArrayList();
         String[] parts = str.split(",");
         for (String part : parts)
         {
            part = part.trim();
            list.add(part);
         }
         return (T) list;
      }
      else
      {
         System.err.println("Can't cast: " + str + " - class " + type.getName());
      }

      return (T) value;
   }

   public static Map<String, LinkedHashSet> getMetaData(Connection conn) throws Exception
   {
      Map tables = new HashMap();
      DatabaseMetaData dbmd = conn.getMetaData();

      ResultSet rs = dbmd.getTables(null, null, null, new String[]{"TABLE"});
      while (rs.next())
      {
         String tableCat = rs.getString("TABLE_CAT");
         String tableSchem = rs.getString("TABLE_SCHEM");
         String tableName = rs.getString("TABLE_NAME");
         ResultSet colsRs = dbmd.getColumns(tableCat, tableSchem, tableName, null);

         LinkedHashSet cols = new LinkedHashSet();
         tables.put(tableName, cols);

         while (colsRs.next())
         {
            String colName = colsRs.getString("COLUMN_NAME");
            cols.add(colName);
         }
      }
      return tables;
   }

   public static List<Field> getFields(Class clazz)
   {
      List<Field> fields = new ArrayList();

      do
      {
         if (clazz.getName().startsWith("java"))
            break;

         Field[] farr = clazz.getDeclaredFields();
         if (farr != null)
         {
            for (Field f : farr)
            {
               f.setAccessible(true);
            }
            fields.addAll(Arrays.asList(farr));
         }
         clazz = clazz.getSuperclass();
      }
      while (clazz != null && !Object.class.equals(clazz));

      return fields;
   }

}
