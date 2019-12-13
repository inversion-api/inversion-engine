/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.inversion.cloud.model.Index;
import io.inversion.cloud.utils.Rows.Row;

/**
 * TODO: http://www.symantec.com/connect/articles/detection-sql-injection-and-cross-site-scripting-attacks
 *
 */
public class SqlUtils
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

   public static char colQuote(Connection conn)
   {
      String connstr = conn.toString().toLowerCase();
      if (connstr.indexOf("mysql") > -1)
         return '`';

      return '"';
   }

   public static String quote(Connection conn, Object str)
   {
      String connstr = conn.toString().toLowerCase();
      if (connstr.indexOf("mysql") > -1)
         return "`" + str + "`";

      return "\"" + str + "\"";
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
      sql.append(quote(conn, tableName)).append(" (");
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

   //note this was parameterized with "List<Map> rows" but could not figure out 
   //how to get compiler to accept passing in a Rows object without removing <Map>
   public static void insertMaps(Connection conn, String tableName, List rows) throws Exception
   {
      LinkedHashSet keySet = new LinkedHashSet();

      for (Map row : ((List<Map>) rows))
      {
         keySet.addAll(row.keySet());
      }

      List<String> keys = new ArrayList(keySet);
      String sql = buildInsertSQL(conn, tableName, keys.toArray());

      Exception ex = null;
      PreparedStatement stmt = conn.prepareStatement(sql);
      try
      {
         notifyBefore("insertMaps", sql, rows);

         for (Map row : ((List<Map>) rows))
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
         ex = e;
         notifyError("insertMaps", sql, rows, ex);
      }
      finally
      {
         SqlUtils.close(stmt);
         notifyAfter("insertMap", sql, rows, ex, null);
      }
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
               namesClause.append(quote(conn, name)).append(",");
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
      sql.append(quote(conn, tableName)).append(" SET ");
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
         sql = new StringBuffer("UPDATE ").append(quote(conn, tableName)).append(" SET ");

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
               sql.append(quote(conn, f.getName())).append(" = ?");
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

   public static Object h2Upsert(Connection conn, String tableName, Index index, Map<String, Object> row) throws Exception
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
      for (int i = 0; i < index.size(); i++)
      {
         keyCols += quote(conn, index.getColumn(i).getName());
         if (i < index.size() - 1)
            keyCols += ", ";
      }

      sql += " MERGE INTO " + quote(conn, tableName) + " (" + SqlUtils.getColumnStr(conn, cols) + ")  KEY(" + keyCols + ") VALUES (" + getQuestionMarkStr(vals.size()) + ")";

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

   public static List<String> mysqlUpsert(Connection conn, String tableName, List<Map<String, Object>> rows) throws Exception
   {
      LinkedHashSet keySet = new LinkedHashSet();
      List<String> primaryKeys = new ArrayList<String>();

      for (Map row : rows)
      {
         keySet.addAll(row.keySet());
      }

      List<String> keys = new ArrayList(keySet);
      String sql = buildInsertOnDuplicateKeySQL(conn, tableName, keys.toArray());

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
            primaryKeys.add(key);
         }
         SqlUtils.close(stmt);
         notifyAfter("upsert", sql, rows, ex, null);
      }
      return primaryKeys;
   }

   public static String buildInsertOnDuplicateKeySQL(Connection conn, String tableName, Object[] columnNameArray)
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
      sql += " DELETE FROM " + quote(conn, table);
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
      sql += " DELETE FROM " + quote(conn, table);
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
         sql = new StringBuffer("DELETE FROM ").append(quote(conn, tableName));

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
                  //                  if (i % 100 == 0)
                  //                     System.out.print("\r\n" + i + "/" + sql.length);
                  //                  else
                  //                     System.out.print(".");

                  try
                  {
                     stmt.execute(sql[i]);
                  }
                  catch (SQLException ex)
                  {
                     //System.out.println("Error trying to run statement: " + ex.getMessage());
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
         sb.append(quote(conn, columnNameArray[i]));
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
         sb.append(quote(conn, columnNameArray[i]));
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
         sb.append(quote(conn, columnNameArray.get(i)));
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

   public static Object cast(Object object, String jdbcType)
   {
      try
      {
         jdbcType = jdbcType.toUpperCase();
         return cast(object, (Integer) Types.class.getField(jdbcType).get(null));
      }
      catch (Exception ex)
      {
         throw new RuntimeException("Error casting to type " + jdbcType + " for value " + object);
      }
   }

   /**
    * https://www.cis.upenn.edu/~bcpierce/courses/629/jdkdocs/guide/jdbc/getstart/mapping.doc.html
    * @param object
    * @param sqlType
    * @return
    */
   public static Object cast(Object object, int jdbcType)
   {
      try
      {
         if (object == null)
            return null;

         switch (jdbcType)
         {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.LONGNVARCHAR:
               return object.toString();
            case Types.CLOB:
               return object.toString().trim();
            case Types.NUMERIC:
            case Types.DECIMAL:
               return new BigDecimal(object.toString());
            case Types.BIT:
            case Types.BOOLEAN:
               return object.toString().toLowerCase().startsWith("t") || object.toString().equals("1");
            case Types.TINYINT:
               return Byte.parseByte(object.toString());
            case Types.SMALLINT:
               return Short.parseShort(object.toString());
            case Types.INTEGER:
               return Integer.parseInt(object.toString());
            case Types.BIGINT:
               return Long.parseLong(object.toString());
            case Types.FLOAT:
            case Types.REAL:
            case Types.DOUBLE:
               return Double.parseDouble(object.toString());
            case Types.DATALINK:
               return new URL(object.toString());

            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
               throw new UnsupportedOperationException("Binary types are currently unsupporrted");

            case Types.DATE:
               return new java.sql.Date(date(object.toString()).getTime());
            case Types.TIMESTAMP:
               return new java.sql.Timestamp(date(object.toString()).getTime());
            default :
               throw new UnsupportedOperationException("JDBC Type: " + jdbcType + " is not yet supported");
         }
      }
      catch (Exception ex)
      {
         throw new RuntimeException("Error casting to type " + jdbcType + " for value " + object);
      }
   }

   public static Date date(String date)
   {
      try
      {
         //not supported in JDK 1.6
         //         DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
         //         TemporalAccessor accessor = timeFormatter.parse(date);
         //         return Date.from(Instant.from(accessor));
         return Utils.parseIso8601(date);
      }
      catch (Exception ex)
      {

      }

      try
      {
         //2018-06-20 01:10:24.0 - mysql timestamp string represnetation
         SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
         return f.parse(date);
      }
      catch (Exception ex)
      {

      }

      try
      {
         SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
         return f.parse(date);

      }
      catch (Exception ex)
      {

      }

      try
      {
         SimpleDateFormat f = new SimpleDateFormat("MM/dd/yy");

         int lastSlash = date.lastIndexOf("/");
         if (lastSlash > 0 && lastSlash == date.length() - 5)
         {
            f = new SimpleDateFormat("MM/dd/yyyy");
         }
         Date d = f.parse(date);
         //System.out.println(d);
         return d;

      }
      catch (Exception ex)
      {

      }

      try
      {
         SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
         return f.parse(date);
      }
      catch (Exception ex)
      {

      }
      throw new RuntimeException("unsupported format: " + date);
   }

}
