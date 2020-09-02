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
package io.inversion.jdbc;

import io.inversion.ApiException;
import io.inversion.utils.Rows;
import io.inversion.utils.Rows.Row;
import io.inversion.utils.Utils;
import org.apache.commons.collections4.CollectionUtils;

import java.io.*;
import java.lang.reflect.Array;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A collection of super helpful JDBC utility methods with SQL injection attack defense built in.
 */
public class JdbcUtils {
    static final String[] ILLEGALS_REGX = new String[]{"insert", "update", "delete", "drop", "truncate", "exec"};

    static final Pattern[]         ILLEGALS  = new Pattern[ILLEGALS_REGX.length];
    static final List<SqlListener> listeners = new ArrayList<>();

    static {
        for (int i = 0; i < ILLEGALS_REGX.length; i++) {
            ILLEGALS[i] = Pattern.compile("\\W*" + ILLEGALS_REGX[i] + "\\W+", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        }
    }

    public static String getDbType(Connection conn) {
        String connstr = conn.toString().toLowerCase();
        if (connstr.contains("mysql"))
            return "mysql";
        if (connstr.contains("postgres"))
            return "postgres";
        if (connstr.contains("h2"))
            return "h2";
        if (connstr.contains("sqlserver"))
            return "sqlserver";
        if (connstr.contains("clientconnectionid"))
            return "sqlserver";

        return "unknown";
    }

    public static char colQuote(Connection conn) {
        String connstr = conn.toString().toLowerCase();
        if (connstr.contains("mysql"))
            return '`';

        return '"';
    }

    public static String quoteCol(Connection conn, Object str) {
        char c = colQuote(conn);
        return c + str.toString() + c;
    }

    public static void addSqlListener(SqlListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public static void removeSqlListener(SqlListener listener) {
        listeners.remove(listener);
    }

    public static void notifyBefore(String method, String sql, Object args) {
        for (SqlListener listener : listeners) {
            listener.beforeStmt(method, sql, args);
        }
    }

    public static void notifyError(String method, String sql, Object args, Exception ex) {
        for (SqlListener listener : listeners) {
            listener.onError(method, sql, args, ex);
        }
    }

    public static void notifyAfter(String method, String sql, Object args, Exception ex, Object result) {
        for (SqlListener listener : listeners) {
            listener.afterStmt(method, sql, args, ex, result);
        }
    }

    public static Object execute(Connection conn, String sql, Object... vals) {
        if (vals != null && vals.length == 1 && vals[0] instanceof Collection)
            vals = ((Collection) vals[0]).toArray();

        notifyBefore("execute", sql, vals);

        Exception ex    = null;
        Statement stmt  = null;
        ResultSet rs    = null;
        Object    rtval = null;

        try {
            if (isSelect(sql)) {
                if (vals != null && vals.length > 0) {
                    stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    for (int i = 0; i < vals.length; i++) {
                        ((PreparedStatement) stmt).setObject(i + 1, vals[i]);
                    }
                    rs = ((PreparedStatement) stmt).executeQuery();
                } else {
                    stmt = conn.createStatement();
                    rs = stmt.executeQuery(sql);
                }
                if (rs.next()) {
                    rtval = rs.getObject(1);
                    return rtval;
                }
            } else {
                if (vals != null && vals.length > 0) {
                    stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    for (int i = 0; i < vals.length; i++) {
                        ((PreparedStatement) stmt).setObject(i + 1, vals[i]);
                    }
                    ((PreparedStatement) stmt).execute();
                } else {
                    stmt = conn.createStatement();
                    stmt.execute(sql, Statement.RETURN_GENERATED_KEYS);
                }

                if (isInsert(sql)) {
                    try {
                        rs = stmt.getGeneratedKeys();
                        if (rs.next()) {
                            rtval = rs.getObject(1);
                            return rtval;
                        }
                    } catch (SQLFeatureNotSupportedException e) {
                        notifyError("execute", sql, vals, e);
                        //do nothing
                    }
                } else if (isUpdate(sql)) {
                    try {
                        rtval = stmt.getUpdateCount();
                        return rtval;
                    } catch (SQLFeatureNotSupportedException e) {
                        notifyError("execute", sql, vals, e);
                        //do nothing
                    }
                }
            }
        } catch (Exception e) {
            notifyError("execute", sql, vals, e);
            ex = new SQLException(e.getMessage() + " SQL=" + sql, Utils.getCause(e));
        } finally {
            close(rs, stmt);
            notifyAfter("execute", sql, vals, ex, rtval);
        }

        return null;
    }

    public static boolean isSelect(String sql) {
        return sql != null && sql.toLowerCase().trim().startsWith("select ");
    }

   /*
   +------------------------------------------------------------------------------+
   | SELECT UTILS
   +------------------------------------------------------------------------------+
    */

    public static Rows selectRows(Connection conn, String sql, Object... vals) throws SQLException {
        if (vals != null && vals.length == 1 && vals[0] instanceof List)
            vals = ((List) vals[0]).toArray();

        notifyBefore("selectRows", sql, vals);

        Exception ex   = null;
        Statement stmt = null;
        ResultSet rs   = null;
        Rows      rows = null;

        try {
            if (vals != null && vals.length > 0) {
                stmt = conn.prepareStatement(sql);
                for (int i = 0; i < vals.length; i++) {
                    Object o = vals[i];
                    ((PreparedStatement) stmt).setObject(i + 1, o);
                }
                rs = ((PreparedStatement) stmt).executeQuery();
            } else {
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
            }

            rows = new Rows();

            if (!sql.toLowerCase().trim().startsWith("set ")) {

                ResultSetMetaData rsmd = rs.getMetaData();
                int               cols = rsmd.getColumnCount();
                for (int i = 1; i <= cols; i++) {
                    rows.addKey(rsmd.getColumnLabel(i));
                }

                while (rs.next()) {
                    rows.addRow();
                    for (int i = 0; i < cols; i++) {
                        Object o = null;
                        try {
                            o = rs.getObject(i + 1);

                            if (o instanceof Clob) {
                                Reader        reader = ((Clob) o).getCharacterStream();
                                char[]        arr    = new char[8 * 1024];
                                StringBuilder buffer = new StringBuilder();
                                int           numCharsRead;
                                while ((numCharsRead = reader.read(arr, 0, arr.length)) != -1) {
                                    buffer.append(arr, 0, numCharsRead);
                                }
                                reader.close();
                                o = buffer.toString();
                            } else if (o != null && o.getClass().isArray() && Array.getLength(o) == 0) {
                                o = null;
                            }

                        } catch (Exception e) {
                            if (ex != null)
                                ex = e;
                            e.printStackTrace();
                            notifyError("selectRows", sql, vals, e);
                        }
                        rows.put(o);
                    }
                }
            }
        } catch (Exception e) {
            if (ex == null)
                ex = e;
        } finally {
            close(stmt, rs);
            notifyAfter("selectRows", sql, vals, ex, rows);
        }
        return rows;
    }

    public static Row selectRow(Connection conn, String sql, Object... vals) throws SQLException {
        Rows rows = selectRows(conn, sql, vals);
        if (rows.size() > 0)
            return rows.get(0);

        return null;
    }

    public static int selectInt(Connection conn, String sql, Object... vals) throws SQLException {
        Object val = selectValue(conn, sql, vals);
        if (val == null)
            return -1;
        return Integer.parseInt(val + "");
    }

    public static Object selectValue(Connection conn, String sql, Object... vals) throws SQLException {
        Row row = selectRow(conn, sql, vals);
        if (row != null) {
            return row.get(row.keySet().iterator().next());
        }
        return null;
    }

    public static boolean isInsert(String sql) {
        String lc = sql.toLowerCase().trim();
        return lc.startsWith("insert ") || lc.startsWith("merge ");
    }

    //   public static <T> T selectObject(Connection conn, String sql, Class<T> clazz, Object... vals) throws SQLException
    //   {
    //      Row row = selectRow(conn, sql, vals);
    //      if (row != null)
    //      {
    //         Object o = clazz.getDeclaredConstructor().newInstance();
    //         poplulate(o, row);
    //         return (T) o;
    //      }
    //
    //      return null;
    //   }

    //   public static Object poplulate(Object o, Map<String, Object> row)
    //   {
    //      for (Field field : getFields(o.getClass()))
    //      {
    //         try
    //         {
    //            Object val = row.get(field.getName());
    //            if (val != null)
    //            {
    //               val = convert(val, field.getType());
    //
    //               if (val != null && val instanceof Collection)
    //               {
    //                  Collection coll = (Collection) field.get(o);
    //                  coll.addAll((Collection) val);
    //               }
    //               else
    //               {
    //                  field.set(o, val);
    //               }
    //            }
    //         }
    //         catch (Exception ex)
    //         {
    //            //OK
    //         }
    //      }
    //
    //      return o;
    //   }

    //   public static <T> T convert(Object value, Class<T> type)
    //   {
    //      if (value == null)
    //         return null;
    //
    //      if (type.isAssignableFrom(value.getClass()))
    //      {
    //         return (T) value;
    //      }
    //
    //      if (type.equals(boolean.class) || type.equals(Boolean.class))
    //      {
    //         if (Number.class.isAssignableFrom(value.getClass()))
    //         {
    //            long num = Long.parseLong(value + "");
    //            if (num <= 0)
    //               return (T) Boolean.FALSE;
    //            else
    //               return (T) Boolean.TRUE;
    //         }
    //         if (value instanceof Boolean)
    //            return (T) value;
    //      }
    //      if (value instanceof Number)
    //      {
    //         if (type.equals(Long.class) || type.equals(long.class))
    //         {
    //            value = ((Number) value).longValue();
    //            return (T) value;
    //         }
    //         else if (type.equals(Integer.class) || type.equals(int.class))
    //         {
    //            value = ((Number) value).intValue();
    //            return (T) value;
    //         }
    //         else if (type.isAssignableFrom(long.class))
    //         {
    //            value = ((Number) value).longValue();
    //            return (T) value;
    //         }
    //      }
    //
    //      String str = value + "";
    //
    //      if (String.class.isAssignableFrom(type))
    //      {
    //         return (T) str;
    //      }
    //      else if (boolean.class.isAssignableFrom(type))
    //      {
    //         str = str.toLowerCase();
    //         return (T) (Boolean) (str.equals("true") || str.equals("t") || str.equals("1"));
    //      }
    //      else if (int.class.isAssignableFrom(type))
    //      {
    //         return (T) (Integer) Integer.parseInt(str);
    //      }
    //      else if (long.class.isAssignableFrom(type))
    //      {
    //         return (T) (Long) Long.parseLong(str);
    //      }
    //      else if (float.class.isAssignableFrom(type))
    //      {
    //         return (T) (Float) Float.parseFloat(str);
    //      }
    //      else if (Collection.class.isAssignableFrom(type))
    //      {
    //         Collection list = new ArrayList<>();
    //         String[] parts = str.split(",");
    //         for (String part : parts)
    //         {
    //            part = part.trim();
    //            list.add(part);
    //         }
    //         return (T) list;
    //      }
    //      else
    //      {
    //         System.err.println("Can't cast: " + str + " - class " + type.getName());
    //      }
    //
    //      return (T) value;
    //   }
    //
    //   public static List<Field> getFields(Class clazz)
    //   {
    //      List<Field> fields = new ArrayList<>();
    //
    //      do
    //      {
    //         if (clazz.getName().startsWith("java"))
    //            break;
    //
    //         Field[] farr = clazz.getDeclaredFields();
    //         if (farr != null)
    //         {
    //            for (Field f : farr)
    //            {
    //               f.setAccessible(true);
    //            }
    //            fields.addAll(Arrays.asList(farr));
    //         }
    //         clazz = clazz.getSuperclass();
    //      }
    //      while (clazz != null && !Object.class.equals(clazz));
    //
    //      return fields;
    //   }

   /*
   +------------------------------------------------------------------------------+
   | INSERT UTILS
   +------------------------------------------------------------------------------+
    */

    public static String buildInsertSQL(Connection conn, String tableName, Object[] columnNameArray) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(quoteCol(conn, tableName)).append(" (");
        sql.append(getColumnStr(conn, columnNameArray)).append(") VALUES (");
        sql.append(getQuestionMarkStr(columnNameArray)).append(")");

        return sql.toString();
    }

    public static Object insertMap(Connection conn, String tableName, Map row) throws SQLException {
        List<Object> keys   = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Object key : row.keySet()) {
            keys.add(key);
            values.add(row.get(key));
        }
        String sql = buildInsertSQL(conn, tableName, keys.toArray());
        return execute(conn, sql, values.toArray());
    }

    public static List insertMaps(Connection conn, String tableName, List maps) throws SQLException {
        if ("sqlserver".equalsIgnoreCase(getDbType(conn)) && maps.size() > 0) {
            //-- as of 2020 sqlserver does not seem to support getGeneratedKeys for multiple rows.
            //--
            //-- https://github.com/microsoft/mssql-jdbc/issues/358
            //-- https://stackoverflow.com/questions/13641832/getgeneratedkeys-after-preparedstatement-executebatch/13642539#13642539
            List returnKeys = new ArrayList<>();
            for (Object map : maps) {
                returnKeys.addAll(insertMaps0(conn, tableName, Collections.singletonList(map)));
            }
            return returnKeys;
        } else {
            return insertMaps0(conn, tableName, maps);
        }
    }

    static List insertMaps0(Connection conn, String tableName, List maps) throws SQLException {
        List<Map<String, Object>> rows = (List<Map<String, Object>>) maps;

        List<Object>          returnKeys = new ArrayList<>();
        LinkedHashSet<String> keys       = new LinkedHashSet<>();

        for (Map row : rows) {
            keys.addAll(row.keySet());
        }

        StringBuilder buff = new StringBuilder("INSERT INTO ");
        buff.append(quoteCol(conn, tableName)).append(" (");
        buff.append(getColumnStr(conn, keys.toArray())).append(") VALUES \r\n");

        for (int i = 0; i < maps.size(); i++) {
            buff.append("(").append(getQuestionMarkStr(keys.size())).append(")");
            if (i < maps.size() - 1)
                buff.append(",\r\n");
        }

        String sql = buff.toString();

        Exception         ex   = null;
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        try {
            notifyBefore("insertMaps", sql, rows);

            int idx = 1;
            for (Map row : rows) {
                for (String col : keys) {
                    Object value = row.get(col);
                    stmt.setObject(idx++, value);
                }

            }
            stmt.execute();
            ResultSet rs = stmt.getGeneratedKeys();
            while (rs.next()) {
                Object key = rs.getObject(1);
                returnKeys.add(key);
            }
        } catch (Exception e) {
            ex = e;
            notifyError("insertMaps", sql, rows, ex);
            throw e;
        } finally {
            notifyAfter("insertMap", sql, rows, ex, null);
        }

        if (returnKeys.size() == 0) {
            //the table must not use an auto increment key.
            for (int i = 0; i < maps.size(); i++)
                returnKeys.add(null);
        }

        if (returnKeys.size() != rows.size())
            throw new RuntimeException("insertMaps() did not return generatedKeys for all rows");

        return returnKeys;
    }

    public static boolean isUpdate(String sql) {
        return sql.toLowerCase().trim().startsWith("update ");
    }

   /*
   +------------------------------------------------------------------------------+
   | UPDATE UTILS
   +------------------------------------------------------------------------------+
    */

    public static List<Integer> update(Connection conn, String tableName, List<String> primaryKeyCols, List<Map<String, Object>> rows) throws Exception {
        List<Integer> updatedCounts = new ArrayList<>();

        Set                       cols  = null;
        List<Map<String, Object>> batch = new ArrayList<>();
        for (Map row : rows) {
            if (cols == null) {
                cols = row.keySet();
            }

            if (batch.size() > 0 && CollectionUtils.disjunction(cols, row.keySet()).size() > 0) {
                updatedCounts.addAll(updateBatch(conn, tableName, primaryKeyCols, batch));
                batch.clear();
            }
            cols = row.keySet();
            batch.add(row);
        }
        if (batch.size() > 0) {
            updatedCounts.addAll(updateBatch(conn, tableName, primaryKeyCols, batch));
        }
        return updatedCounts;
    }

    public static List<Integer> updateBatch(Connection conn, String tableName, List<String> keyCols, List<Map<String, Object>> rows) throws SQLException {
        if (rows.size() == 0)
            return Collections.EMPTY_LIST;

        List<Integer> returnCounts = new ArrayList<>();

        List<String> valCols = new ArrayList(rows.get(0).keySet());
        valCols.removeAll(keyCols);

        String sql = buildUpdateSQL(conn, tableName, valCols.toArray(), keyCols.toArray());

        Exception         ex   = null;
        PreparedStatement stmt = conn.prepareStatement(sql);
        try {
            notifyBefore("update", sql, rows);

            for (Map<String, Object> row : rows) {
                for (int i = 0; i < valCols.size(); i++) {
                    Object value = row.get(valCols.get(i));
                    stmt.setObject(i + 1, value);
                }

                for (int i = 0; i < keyCols.size(); i++) {
                    Object value = row.get(keyCols.get(i));
                    stmt.setObject(i + 1 + valCols.size(), value);
                }

                stmt.addBatch();
            }
            int[] updatedCounts = stmt.executeBatch();
            for (int updatedCount : updatedCounts) {
                returnCounts.add(updatedCount);
            }
        } catch (Exception e) {
            ex = e;
            notifyError("update", sql, rows, ex);
            throw e;
        } finally {
            JdbcUtils.close(stmt);
            notifyAfter("update", sql, rows, ex, returnCounts);
        }
        return returnCounts;
    }

    public static String buildUpdateSQL(Connection conn, String tableName, Object[] setColumnNameArray, Object[] whereColumnNames) {
        // UPDATE tmtuple SET model_id = ? , subj = ? , pred = ? , obj = ? , declared = ?

        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(quoteCol(conn, tableName)).append(" SET ");
        sql.append(getWhereColumnStr(conn, setColumnNameArray, ","));
        if (whereColumnNames != null && whereColumnNames.length > 0) {
            sql.append(" WHERE ").append(getWhereColumnStr(conn, whereColumnNames, " AND "));
        }
        return sql.toString();
    }

    /**
     * Batches <code>rows</code> into groups containing identical keys and then
     * inserts rows that are missing indexCols key values or attempts an upsert
     * for rows that have the key values...the row could have the key but still
     * not exist in the db in cases where the key is not an autoincrement number.
     *
     * @param conn           the jdbc connection
     * @param tableName      the table to upsert into
     * @param primaryKeyCols a unique key for the table that will constrain the upsert
     * @param rows           the data to upsert
     * @return the primaryKeyCols values for all rows upserted
     * @throws SQLException when the upsert fails
     */
    public static List<Row> upsert(Connection conn, String tableName, List<String> primaryKeyCols, List<Map<String, Object>> rows) throws SQLException {
        List<Row> generatedKeys = new ArrayList<>();
        if (rows.isEmpty())
            return Collections.EMPTY_LIST;

        Set                       cols   = null;
        int                       hadKey = -1;
        List<Map<String, Object>> batch  = new ArrayList<>();
        for (Map row : rows) {
            int hasKey = 1;
            for (String indexCol : primaryKeyCols) {
                if (Utils.empty(row.get(indexCol))) {
                    hasKey = 0;
                    break;
                }
            }
            if (hadKey == -1)
                hadKey = hasKey;

            if (cols == null) {
                cols = row.keySet();
            }

            if (batch.size() > 0 && (hadKey != hasKey) || CollectionUtils.disjunction(cols, row.keySet()).size() > 0) {
                if (hadKey == 0)
                    generatedKeys.addAll(insertBatch(conn, tableName, primaryKeyCols, batch));
                else {
                    generatedKeys.addAll(upsertBatch(conn, tableName, primaryKeyCols, batch));
                }

                batch.clear();
            }

            hadKey = hasKey;
            cols = row.keySet();
            batch.add(row);
        }

        if (batch.size() > 0) {
            if (hadKey == 0)
                generatedKeys.addAll(insertBatch(conn, tableName, primaryKeyCols, batch));
            else
                generatedKeys.addAll(upsertBatch(conn, tableName, primaryKeyCols, batch));
        }

        for (int i = 0; i < generatedKeys.size(); i++) {
            Row row = new Row();
            for (String col : primaryKeyCols) {
                Object val = rows.get(i).get(col);
                if (val == null) {
                    val = generatedKeys.get(i);
                    generatedKeys.set(i, null);
                }

                if (val == null)
                    throw ApiException.new500InternalServerError("Unable to determine upsert key or column '{}'", col);

                row.put(col, val);
            }
            generatedKeys.set(i, row);
        }

        return generatedKeys;
    }

   /*
   +------------------------------------------------------------------------------+
   | UPSERT UTILS
   +------------------------------------------------------------------------------+
    */

    static List insertBatch(Connection conn, String tableName, List<String> indexCols, List<Map<String, Object>> rows) throws SQLException {
        List returnKeys = insertMaps(conn, tableName, rows);
        for (int i = 0; i < returnKeys.size(); i++) {
            Object key = returnKeys.get(i);
            if (key == null) {
                key = rows.get(i).get(indexCols.get(0));
                if (key == null)
                    throw ApiException.new500InternalServerError("Unable to determine key for row: " + rows.get(i));

                returnKeys.set(i, key);
            }
        }
        return returnKeys;
    }

    static List upsertBatch(Connection conn, String tableName, List<String> idxCols, List<Map<String, Object>> rows) throws SQLException {
        List   returnKeys = new ArrayList<>();
        String type       = getDbType(conn);

        switch (type) {
            case "mysql":
                mysqlUpsertBatch(conn, tableName, idxCols, rows);
                break;

            case "postgres":
                postgresUpsertBatch(conn, tableName, idxCols, rows);
                break;

            case "sqlserver":
                sqlserverUpsertBatch(conn, tableName, idxCols, rows);
                break;

            default:
                h2UpsertBatch(conn, tableName, idxCols, rows);
                break;
        }

        for (Map row : rows) {
            Object key = row.get(idxCols.get(0));
            if (key == null)
                System.out.println("Unable to determine key for row: " + row);

            returnKeys.add(key);
        }
        return returnKeys;

    }

    static List h2UpsertBatch(Connection conn, String tableName, List<String> idxCols, List<Map<String, Object>> rows) throws SQLException {
        List returnKeys = new ArrayList<>();
        for (Map row : rows) {
            returnKeys.add(h2UpsertBatch(conn, tableName, idxCols, row));
        }
        return returnKeys;
    }

    static Object h2UpsertBatch(Connection conn, String tableName, List<String> idxCols, Map<String, Object> row) throws SQLException {
        String sql = "";

        List<String> cols = new ArrayList<>();
        List<Object> vals = new ArrayList<>();
        for (String col : row.keySet()) {
            cols.add(col);
            vals.add(row.get(col));
        }

        StringBuilder keyCols = new StringBuilder();
        for (int i = 0; i < idxCols.size(); i++) {
            keyCols.append(quoteCol(conn, idxCols.get(i)));
            if (i < idxCols.size() - 1)
                keyCols.append(", ");
        }

        sql += " MERGE INTO " + quoteCol(conn, tableName) + " (" + JdbcUtils.getColumnStr(conn, cols) + ")  KEY(" + keyCols + ") VALUES (" + getQuestionMarkStr(vals.size()) + ")";

        Exception ex = null;
        try {
            notifyBefore("upsert", sql, row);
            return execute(conn, sql, vals);
        } catch (Exception e) {
            ex = e;
            notifyError("upsert", sql, row, ex);
            throw e;
        } finally {
            notifyAfter("upsert", sql, row, ex, null);
        }
    }

    static void mysqlUpsertBatch(Connection conn, String tableName, List<String> idxCols, List<Map<String, Object>> rows) throws SQLException {

        LinkedHashSet<String> keySet = new LinkedHashSet<>();
        for (Map row : rows) {
            keySet.addAll(row.keySet());
        }
        ArrayList<String> keys = new ArrayList<>(keySet);

        String sql = mysqlBuildInsertOnDuplicateKeySQL(conn, tableName, keys.toArray());

        Exception         ex   = null;
        PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
        try {
            notifyBefore("upsert", sql, rows);

            for (Map<String, Object> row : rows) {
                for (int i = 0; i < keys.size(); i++) {
                    Object value = row.get(keys.get(i));
                    stmt.setObject(i + 1, value);
                }
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (Exception e) {
            ex = e;
            notifyError("upsert", sql, rows, ex);
            throw e;
        } finally {
            close(stmt);
            notifyAfter("upsert", sql, rows, ex, null);
        }

    }

    static String mysqlBuildInsertOnDuplicateKeySQL(Connection conn, String tableName, Object[] columnNameArray) {
        StringBuilder sql = new StringBuilder(buildInsertSQL(conn, tableName, columnNameArray));
        sql.append(" ON DUPLICATE KEY UPDATE ");
        for (int i = 0; i < columnNameArray.length; i++) {
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
     * @param conn      the connection
     * @param tableName the table
     * @param rows      the values to upsert
     * @return ids of the modified rows
     * @throws SQLException when the upsert fails
     */
    static List postgresUpsertBatch(Connection conn, String tableName, List<String> idxCols, List<Map<String, Object>> rows) throws SQLException {
        List<Object> returnKeys = new ArrayList<>();
        List<String> cols       = new ArrayList<>(rows.get(0).keySet());

        StringBuilder buff = new StringBuilder(buildInsertSQL(conn, tableName, cols.toArray()));
        buff.append("\r\n ON CONFLICT (");
        for (int i = 0; i < idxCols.size(); i++) {
            buff.append(quoteCol(conn, idxCols.get(i)));
            if (i < idxCols.size() - 1)
                buff.append(", ");
        }
        buff.append(") DO UPDATE SET ");
        for (int i = 0; i < cols.size(); i++) {
            buff.append("\r\n ").append(quoteCol(conn, cols.get(i))).append(" = EXCLUDED.").append(quoteCol(conn, cols.get(i)));
            if (i < cols.size() - 1)
                buff.append(", ");
        }

        Exception         ex   = null;
        String            sql  = buff.toString();
        PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
        try {
            notifyBefore("upsert", sql, rows);

            for (Map<String, Object> row : rows) {
                for (int i = 0; i < cols.size(); i++) {
                    Object value = row.get(cols.get(i));
                    stmt.setObject(i + 1, value);
                }
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (Exception e) {
            ex = e;
            notifyError("upsert", sql, rows, ex);
            throw e;
        } finally {
            ResultSet rs = stmt.getGeneratedKeys();
            while (rs.next()) {

                String key = rs.getString(1);
                returnKeys.add(key);
            }
            close(stmt);
            notifyAfter("upsert", sql, rows, ex, null);
        }
        return returnKeys;
    }

    /*
     * https://stackoverflow.com/questions/108403/solutions-for-insert-or-update-on-sql-server
     */
    static List sqlserverUpsertBatch(Connection conn, String tableName, List<String> idxCols, List<Map<String, Object>> rows) throws SQLException {
        List<Object> returnKeys = new ArrayList<>();
        for (Map row : rows) {
            sqlserverUpsertBatch(conn, tableName, idxCols, row);
            returnKeys.add(row.get(idxCols.get(0)));
        }
        return returnKeys;
    }

    /*
     * UPDATE "orders" SET "CustomerID" = ? , "ShipCity" = ? , "ShipCountry" = ?  WHERE "OrderID" = ?
     * IF @@ROWCOUNT = 0
     * INSERT INTO "orders" ("OrderID", "CustomerID", "ShipCity", "ShipCountry") VALUES (?,?,?,?)
     */
    static void sqlserverUpsertBatch(Connection conn, String tableName, List<String> indexCols, Map<String, Object> row) throws SQLException {
        List<String> updateCols = new ArrayList<>(row.keySet());
        List<String> insertCols = new ArrayList<>(row.keySet());

        if (indexCols.size() < updateCols.size())
            updateCols.removeAll(indexCols);

        String sql = buildUpdateSQL(conn, tableName, updateCols.toArray(), indexCols.toArray());

        sql += "\r\n IF @@ROWCOUNT = 0 ";
        sql += "\r\n " + buildInsertSQL(conn, tableName, insertCols.toArray());

        Exception ex = null;

        PreparedStatement stmt = conn.prepareStatement(sql);
        try {
            notifyBefore("upsert", sql, row);

            int colNum = 1;
            for (String col : updateCols) {
                Object value = row.get(col);
                stmt.setObject(colNum++, value);
            }
            for (String key : indexCols) {
                Object value = row.get(key);
                stmt.setObject(colNum++, value);
            }
            for (String col : insertCols) {
                Object value = row.get(col);
                stmt.setObject(colNum++, value);
            }

            stmt.execute();
        } catch (Exception e) {
            ex = e;
            notifyError("upsert", sql, row, ex);
            throw e;
        } finally {
            close(stmt);
            notifyAfter("upsert", sql, row, ex, null);
        }
    }

    public static void runSql(Connection conn, String sqlString) throws SQLException {
        runSql(conn, readSql(sqlString));
    }

   /*
   +------------------------------------------------------------------------------+
   | BATCH RUNNER UTILS
   +------------------------------------------------------------------------------+
    */

    public static void runSql(Connection conn, InputStream ddlStream) throws SQLException {
        List<String> script = readSql(ddlStream);
        runSql(conn, script.toArray(new String[0]));
    }

    public static List<String> readSql(String string) throws SQLException {
        return readSql(new ByteArrayInputStream(string.getBytes()));
    }

    /**
     * Breaks the input stream up into a list of sql statements where statements are
     * terminated by ";".  Lines starting with "--" or "#" are considred comments are
     * are ignored.
     *
     * @param ddlStream a stream of ddl text
     * @return the text broken into individual sql statements
     * @throws SQLException when io fails
     */
    public static List<String> readSql(InputStream ddlStream) throws SQLException {
        try {
            BufferedReader br      = new BufferedReader(new InputStreamReader(ddlStream));
            String         line;
            StringBuilder  curLine = new StringBuilder();
            List<String>   ddlList = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.length() == 0 || line.startsWith("--") || line.startsWith("#"))
                    continue;

                curLine.append("\r\n").append(line);
                if (line.trim().endsWith(";")) {
                    ddlList.add(curLine.toString().trim());
                    curLine = new StringBuilder();
                }
            }
            if (!Utils.empty(curLine.toString().trim())) //the final statement was not terminated with a ";"
                ddlList.add(curLine.toString().trim());

            return ddlList;
        } catch (IOException io) {
            throw new SQLException("Error reading input stream. " + io.getMessage());
        }
    }

    public static void runSql(Connection con, List<String> sql) throws SQLException {
        runSql(con, sql.toArray(new String[0]));
    }

    public static void runSql(Connection con, String[] sql) throws SQLException {
        //System.out.print("running ddl: ");

        if (sql != null && sql.length > 0) {
            boolean oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                try (Statement stmt = con.createStatement()) {
                    for (String s : sql) {
                        try {
                            stmt.execute(s);
                        } catch (SQLException ex) {
                            System.err.println("Error trying to run sql statement: \r\n" + s + "\r\n\r\n");
                            ex.printStackTrace();
                            throw ex;
                        }
                    }
                }
                con.commit();
            } finally {
                con.setAutoCommit(oldAutoCommit);
            }
        }
        //System.out.println(".done");
    }

    public static String getWhereColumnStr(Connection conn, Object[] columnNameArray, String sep) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < columnNameArray.length; i++) {
            sb.append(quoteCol(conn, columnNameArray[i]));
            sb.append(" = ? ");
            if (i < columnNameArray.length - 1) {
                sb.append(sep).append(" ");
            }
        }

        return sb.toString();
    }

   /*
   +------------------------------------------------------------------------------+
   | MISC UTILS
   +------------------------------------------------------------------------------+
    */

    public static String getColumnStr(Connection conn, Object[] columnNameArray) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < columnNameArray.length; i++) {
            sb.append(quoteCol(conn, columnNameArray[i]));
            if (i < columnNameArray.length - 1) {
                sb.append(", ");
            }
        }

        return sb.toString();
    }

    public static String getColumnStr(Connection conn, List columnNameArray) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < columnNameArray.size(); i++) {
            sb.append(quoteCol(conn, columnNameArray.get(i)));
            if (i < columnNameArray.size() - 1) {
                sb.append(", ");
            }
        }

        return sb.toString();
    }

    public static String getQuestionMarkStr(Object[] columnNameArray) {
        return getQuestionMarkStr(columnNameArray.length);
    }

    public static String getQuestionMarkStr(int numQMarks) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < numQMarks; i++) {
            sb.append("?");
            if (i < numQMarks - 1) {
                sb.append(",");
            }
        }

        return sb.toString();
    }

    public static String check(Object sql) {
        if (sql == null)
            return null;

        String str = sql.toString();

        for (int i = 0; i < ILLEGALS.length; i++) {
            Matcher m = ILLEGALS[i].matcher(str);
            if (m.find())
                throw new RuntimeException("Sql injection attack blocker on keyword \"" + ILLEGALS_REGX[i].trim() + "\".  You have modifying sql in a select statement: " + str);
        }
        return str;
    }

    public static void close(Object... toClose) {
        for (Object o : toClose) {
            try {
                if (o instanceof Connection)
                    ((Connection) o).close();
                else if (o instanceof Statement)
                    ((Statement) o).close();
                else if (o instanceof ResultSet)
                    ((ResultSet) o).close();
            } catch (Exception ex) {
                //ex.printStackTrace();
            }
        }
    }

    public interface SqlListener {
        void onError(String method, String sql, Object args, Exception ex);

        void beforeStmt(String method, String sql, Object args);

        void afterStmt(String method, String sql, Object args, Exception ex, Object result);
    }

}
