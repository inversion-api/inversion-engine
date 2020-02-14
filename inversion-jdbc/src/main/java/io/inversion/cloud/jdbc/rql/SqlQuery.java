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
package io.inversion.cloud.jdbc.rql;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.inversion.cloud.jdbc.db.JdbcDb;
import io.inversion.cloud.jdbc.utils.JdbcUtils;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.Db;
import io.inversion.cloud.model.Property;
import io.inversion.cloud.model.Results;
import io.inversion.cloud.model.Status;
import io.inversion.cloud.rql.Group;
import io.inversion.cloud.rql.Order;
import io.inversion.cloud.rql.Order.Sort;
import io.inversion.cloud.rql.Page;
import io.inversion.cloud.rql.Query;
import io.inversion.cloud.rql.Select;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.rql.Where;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.utils.Rows;
import io.inversion.cloud.utils.Rows.Row;
import io.inversion.cloud.utils.Utils;

public class SqlQuery<D extends Db> extends Query<SqlQuery, D, Select<Select<Select, SqlQuery>, SqlQuery>, Where<Where<Where, SqlQuery>, SqlQuery>, Group<Group<Group, SqlQuery>, SqlQuery>, Order<Order<Order, SqlQuery>, SqlQuery>, Page<Page<Page, SqlQuery>, SqlQuery>>
{
   protected char              stringQuote = '\'';
   protected char              columnQuote = '"';

   String                      selectSql   = null;

   String                      type        = null;

   LinkedHashMap<String, Term> joins;

   public SqlQuery()
   {

   }

   public SqlQuery(Collection table, List<Term> terms)
   {
      super(table, terms);
   }

   protected boolean addTerm(String token, Term term)
   {
      if (term.hasToken("eq"))
      {
         String name = term.getToken(0);

         if (name.endsWith(".select"))
            return true;

         //ignore extraneous name=value pairs if 'name' is not a column
         if (name.indexOf(".") < 0 && collection != null && collection.getProperty(name) == null)
            return true;
      }

      if (joins == null)
         joins = new LinkedHashMap();

      if (term.hasToken("join"))
      {
         //a map is used to avoid adding duplicate joins
         String key = term.toString();
         if (!joins.containsKey(key))
         {
            joins.put(key, term);
         }
         return true;
      }

      return super.addTerm(token, term);
   }

   public Results<Row> doSelect() throws Exception
   {
      JdbcDb db = (JdbcDb) getDb();
      String sql = getPreparedStmt();

      Results results = new Results(this);
      List values = getColValues();

      //-- for test cases and query explain
      String debug = getClass().getSimpleName() + " " + getType() + ": " + sql + " args=" + values;
      debug = debug.replaceAll("\r", "");
      debug = debug.replaceAll("\n", " ");
      debug = debug.replaceAll(" +", " ");
      Chain.debug(debug);
      results.withTestQuery(debug);

      if (!isDryRun())
      {
         Connection conn = db.getConnection();
         //-- prepared statement variables are computing during the 
         //-- generation of the prepared statement above

         try
         {
            Rows rows = JdbcUtils.selectRows(conn, sql, values);
            int foundRows = -1;

            if (Chain.peek().get("foundRows") == null && Chain.first().getRequest().isMethod("GET"))
            {
               if (rows.size() == 0)
               {
                  foundRows = 0;
               }
               else
               {
                  foundRows = queryFoundRows(conn, sql, values);
               }

               Chain.peek().put("foundRows", foundRows);
            }

            results.withFoundRows(foundRows);
            results.withRows(rows);
         }
         catch (Exception ex)
         {
            System.out.println(sql);
            ex.printStackTrace();

            throw new ApiException(Status.SC_500_INTERNAL_SERVER_ERROR, ex.getMessage());
         }
      }

      return results;
   }

   public String getPreparedStmt()
   {
      return toSql(true);
   }

   public String getDynamicStmt()
   {
      return toSql(false);
   }

   protected String toSql(boolean preparedStmt)
   {
      clearValues();
      Parts parts = new Parts();

      printInitialSelect(parts, this.selectSql);
      printTermsSelect(parts, preparedStmt);
      printJoins(parts, joins);
      printWhereClause(parts, getWhere().getFilters(), preparedStmt);
      printGroupClause(parts, find("group"));
      printOrderClause(parts, getOrder().getSorts());
      printLimitClause(parts, getPage().getOffset(), getPage().getLimit());

      return printSql(parts);
   }

   protected String printSql(Parts parts)
   {
      //--compose the final statement
      String buff = parts.select;

      buff += " \r\n" + parts.from;

      if (parts.where != null)
         buff += " \r\n" + parts.where;

      if (parts.select.toLowerCase().startsWith("select "))
      {
         if (parts.group != null)
            buff += " \r\n" + parts.group;

         if (parts.order != null)
            buff += " \r\n" + parts.order;

         if (parts.limit != null)
            buff += " \r\n" + parts.limit;
      }

      return buff.toString();
   }

   protected String printInitialSelect(Parts parts, String initialSelect)
   {
      if (initialSelect == null)
      {
         String quotedTable = printTable();

         if (joins != null && joins.size() > 0)
         {
            Map joined = new HashMap();

            initialSelect = "SELECT DISTINCT " + quotedTable + ".* FROM " + quotedTable;

            for (Entry<String, Term> joinEntry : joins.entrySet())
            {
               Term join = joinEntry.getValue();
               String tableName = join.getToken(0);
               String tableAlias = join.getToken(1);

               if (!joined.containsKey(tableAlias))
               {
                  joined.put(tableAlias, tableName);
                  initialSelect += ", " + quoteCol(tableName) + " " + quoteCol(tableAlias);
               }
            }
         }
         else
         {
            boolean distinct = find("distinct") != null;

            initialSelect = " SELECT " + (distinct ? "DISTINCT " : "") + quotedTable + ".* FROM " + quotedTable;
            //initialSelect = " SELECT FROM " + quotedTable;
         }
      }

      parts.withSql(initialSelect);
      return initialSelect;
   }

   protected String printTermsSelect(Parts parts, boolean preparedStmt)
   {
      StringBuffer cols = new StringBuffer();

      List<Term> terms = getSelect().columns();
      for (int i = 0; i < terms.size(); i++)
      {
         Term term = terms.get(i);
         if (term.hasToken("as"))
         {
            Term function = term.getTerm(0);
            cols.append(" ").append(printTerm(function, null, preparedStmt));

            String colName = term.getToken(1);
            if (!(empty(colName) || colName.indexOf("$$$ANON") > -1))
            {
               //TODO: validate if this mysql special case is required...will require changing tests if removed
               if (db == null || db.isType("mysql"))
                  cols.append(" AS " + asString(colName));
               else
                  cols.append(" AS " + quoteCol(colName));
            }
         }
         else if (term.getToken().indexOf(".") < 0)
         {
            cols.append(" " + printCol(term.getToken()));
         }

         if (i < terms.size() - 1)
            cols.append(", ");
      }

      if (cols.length() > 0)
      {
         boolean restrictCols = find("includes", "sum", "min", "max", "count", "function", "aggregate") != null;
         if (db == null || db.isType("mysql"))
         {
            //-- this a special case for legacy mysql that does not require grouping each column when aggregating 
            restrictCols = find("includes") != null;
         }

         int star = parts.select.lastIndexOf("* ");
         if (restrictCols && star > 0)
         {
            //inserts the select list before the *
            int idx = parts.select.substring(0, star).indexOf(" ");
            String newSelect = parts.select.substring(0, idx) + cols + parts.select.substring(star + 1, parts.select.length());
            parts.select = newSelect;
         }
         else
         {
            String select = parts.select.trim();
            if (!select.toLowerCase().endsWith("select"))
               select += ", ";
            select += cols;

            parts.select = select;
         }
      }

      if (getSelect().isDistinct() && parts.select.toLowerCase().indexOf("distinct") < 0)
      {
         int idx = parts.select.toLowerCase().indexOf("select") + 6;
         parts.select = parts.select.substring(0, idx) + " DISTINCT " + parts.select.substring(idx, parts.select.length());
      }

      if (Chain.peek() != null && Chain.peek().get("foundRows") == null && "mysql".equalsIgnoreCase(getType()) && parts.select.toLowerCase().trim().startsWith("select"))
      {
         int idx = parts.select.toLowerCase().indexOf("select") + 6;
         parts.select = parts.select.substring(0, idx) + " SQL_CALC_FOUND_ROWS " + parts.select.substring(idx, parts.select.length());
      }

      return parts.select;
   }

   protected String printJoins(Parts parts, LinkedHashMap<String, Term> joins)
   {
      if (joins != null)
      {
         for (Entry<String, Term> joinTerm : joins.entrySet())
         {
            Term join = joinTerm.getValue();

            String where = "";

            for (int j = 2; j < join.size(); j += 4)//the first token is the related name, the second token is the table alias
            {
               if (j > 2)
                  where += " AND ";
               where += quoteCol(join.getToken(j)) + "." + quoteCol(join.getToken(j + 1)) + " = " + quoteCol(join.getToken(j + 2)) + "." + quoteCol(join.getToken(j + 3));
            }

            if (where != null)
            {
               where = "(" + where + ")";

               if (empty(parts.where))
                  parts.where = " WHERE " + where;
               else
                  parts.where += " AND " + where;
            }
         }
      }
      return parts.where;
   }

   protected String printWhereClause(Parts parts, List<Term> terms, boolean preparedStmt)
   {
      for (int i = 0; i < terms.size(); i++)
      {
         Term term = terms.get(i);

         String where = printTerm(term, null, preparedStmt);
         if (where != null)
         {
            if (empty(parts.where))
               parts.where = " WHERE " + where;
            else
               parts.where += " AND " + where;
         }
      }
      return parts.where;
   }

   protected String printGroupClause(Parts parts, Term groupBy)
   {
      if (groupBy != null)
      {
         if (parts.group == null)
            parts.group = "GROUP BY ";

         for (Term group : groupBy.getTerms())
         {
            if (!parts.group.endsWith("GROUP BY "))
               parts.group += ", ";
            parts.group += printCol(group.getToken());
         }
      }

      return parts.group;
   }

   protected String printOrderClause(Parts parts, List<Sort> sorts)
   {
      //-- before printing the "order by" statement, if the users did not supply 
      //-- any sort terms but the primary index key is being selected, sort on the 
      //-- primary index.
      //-- TODO: can this be moved into the Order builder?

      if (sorts.isEmpty())
      {
         sorts = getDefaultSorts(parts);
      }

      for (int i = 0; i < sorts.size(); i++)
      {
         //-- now setup the "ORDER BY" clause based on the
         //-- "sort" parameter.  Multiple sorts can be 
         //-- comma separated and a leading '-' indicates
         //-- descending sort for that field
         //--
         //-- ex: "sort=firstName,-age"

         Sort sort = sorts.get(i);
         if (parts.order == null)
            parts.order = "ORDER BY ";

         if (!parts.order.endsWith("ORDER BY "))
            parts.order += ", ";

         parts.order += printCol(sort.getProperty()) + (sort.isAsc() ? " ASC" : " DESC");
      }
      return parts.order;
   }

   protected List<Sort> getDefaultSorts(Parts parts)
   {
      List<Sort> sorts = new ArrayList();

      if (collection != null && collection.getPrimaryIndex() != null)
      {
         for (int k = 0; k < collection.getPrimaryIndex().size(); k++)
         {
            Property col = collection.getPrimaryIndex().getColumn(k);
            if ((parts.select.indexOf("* ") >= 0 || parts.select.indexOf("*,") >= 0)//this trailing space or comma is important because otherwise this would incorrectly match "COUNT(*)" 
                  || parts.select.contains(col.getColumnName()))
            {
               Sort sort = new Sort(col.getColumnName(), true);
               sorts.add(sort);
            }
            else
            {
               sorts.clear();
               break;
            }
         }
      }
      return sorts;
   }

   protected String printLimitClause(Parts parts, int offset, int limit)
   {
      String s = null;
      if (limit >= 0 || offset >= 0)
      {
         if (db == null || getDb().isType("mysql"))
         {
            s = "LIMIT ";
            if (offset > 0)
               s += offset;

            if (limit >= 0)
            {
               if (!s.endsWith("LIMIT "))
                  s += ", ";

               s += limit;
            }
         }
         else if (getDb().isType("sqlserver"))
         {
            //https://docs.microsoft.com/en-us/sql/t-sql/queries/select-order-by-clause-transact-sql?view=sql-server-2017#Offset
            //OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY  

            s = "";
            if (offset >= 0)
               s += " OFFSET " + offset + " ROWS ";

            if (limit >= 0)
               s += " FETCH NEXT " + limit + " ROWS ONLY ";
         }
         else
         {
            s = "";

            if (limit >= 0)
               s += " LIMIT " + limit;

            if (offset >= 0)
               s += " OFFSET " + offset;
         }
      }

      parts.limit = s;
      return s;
   }

   protected int queryFoundRows(Connection conn, String sql, List values) throws Exception
   {
      int foundRows = 0;
      if (db.isType("mysql"))
      {
         sql = "SELECT FOUND_ROWS()";
         foundRows = JdbcUtils.selectInt(conn, sql);
      }
      else
      {
         if (sql.indexOf("LIMIT ") > 0)
            sql = sql.substring(0, sql.lastIndexOf("LIMIT "));

         if (sql.indexOf("OFFSET ") > 0)
            sql = sql.substring(0, sql.lastIndexOf("OFFSET "));

         if (sql.indexOf("ORDER BY ") > 0)
            sql = sql.substring(0, sql.lastIndexOf("ORDER BY "));

         sql = "SELECT count(1) FROM ( " + sql + " ) as q";

         foundRows = JdbcUtils.selectInt(conn, sql, values);
      }
      return foundRows;
   }

   protected String printTerm(Term term, String col, boolean preparedStmt)
   {
      if (term.isLeaf())
      {
         String token = term.getToken();

         String value = null;
         if (isCol(term))
         {
            value = printCol(token);
         }
         else if (isNum(term))
         {
            value = asNum(token);
         }
         else
         {
            value = asString(term);
         }
         return value;
      }

      for (Term child : term.getTerms())
      {
         if (isCol(child))
         {
            col = child.token;
            break;
         }
      }

      List<String> dynamicSqlChildText = new ArrayList();
      for (Term t : term.getTerms())
      {
         dynamicSqlChildText.add(printTerm(t, col, preparedStmt));
      }

      List<String> preparedStmtChildText = new ArrayList(dynamicSqlChildText);

      //allows for callers to substitute callable statement "?"s
      //and/or to account for data type conversion 
      if (preparedStmt)
      {
         for (int i = 0; i < term.size(); i++)
         {
            Term t = term.getTerm(i);
            if (t.isLeaf())
            {
               String val = preparedStmtChildText.get(i);
               if (val.charAt(0) != columnQuote)
               {
                  val = Utils.dequote(val);//go back to the unprinted/quoted version
                  preparedStmtChildText.set(i, replace(term, t, i, col, val));
               }
            }
         }
      }

      return printExpression(term, dynamicSqlChildText, preparedStmtChildText);

   }

   /**
    * Override to handle printing additional functions or to change the 
    * way a specific function is printed.
    *  
    * @param term
    * @param dynamicSqlChildText
    * @param preparedStmtChildText
    * @return
    */
   protected String printExpression(Term term, List<String> dynamicSqlChildText, List<String> preparedStmtChildText)
   {
      String token = term.getToken();

      StringBuffer sql = new StringBuffer("");

      if (term.hasToken("eq", "ne", "like", "w", "sw", "ew", "wo"))
      {
         boolean negation = term.hasToken("ne", "nw", "wo");

         if (term.size() > 2 || negation)
            sql.append("(");

         if (negation)
            sql.append("NOT (");

         String string0 = preparedStmtChildText.get(0);

         for (int i = 1; i < term.size(); i++)
         {
            String stringI = preparedStmtChildText.get(i);
            if ("null".equalsIgnoreCase(stringI))
            {
               sql.append(string0).append(" IS NULL ");
            }
            else
            {
               boolean wildcard = dynamicSqlChildText.get(i).indexOf('%') >= 0;

               if (wildcard)
               {
                  sql.append(string0).append(" LIKE ").append(stringI);
               }
               else
               {
                  sql.append(string0).append(" = ").append(stringI);
               }
            }

            if (i < term.size() - 1)
               sql.append(" OR ");
         }

         if (term.size() > 2 || negation)
            sql.append(")");

         if (negation)
            sql.append(")");
      }
      else if ("nn".equalsIgnoreCase(token))
      {
         sql.append(concatAll(" AND ", " IS NOT NULL", preparedStmtChildText));
      }
      else if ("emp".equalsIgnoreCase(token))
      {
         sql.append("(").append(preparedStmtChildText.get(0)).append(" IS NULL OR ").append(preparedStmtChildText.get(0)).append(" = ").append(asString("")).append(")");
      }
      else if ("nemp".equalsIgnoreCase(token))
      {
         sql.append("(").append(preparedStmtChildText.get(0)).append(" IS NOT NULL AND ").append(preparedStmtChildText.get(0)).append(" != ").append(asString("")).append(")");
      }
      else if ("n".equalsIgnoreCase(token))
      {
         sql.append(concatAll(" AND ", " IS NULL", preparedStmtChildText));
      }
      else if ("lt".equalsIgnoreCase(token))
      {
         sql.append(preparedStmtChildText.get(0)).append(" < ").append(preparedStmtChildText.get(1));
      }
      else if ("le".equalsIgnoreCase(token))
      {
         sql.append(preparedStmtChildText.get(0)).append(" <= ").append(preparedStmtChildText.get(1));
      }
      else if ("gt".equalsIgnoreCase(token))
      {
         sql.append(preparedStmtChildText.get(0)).append(" > ").append(preparedStmtChildText.get(1));
      }
      else if ("ge".equalsIgnoreCase(token))
      {
         sql.append(preparedStmtChildText.get(0)).append(" >= ").append(preparedStmtChildText.get(1));
      }
      else if ("in".equalsIgnoreCase(token) || "out".equalsIgnoreCase(token))
      {
         sql.append(preparedStmtChildText.get(0));

         if ("out".equalsIgnoreCase(token))
            sql.append(" NOT");

         sql.append(" IN(");
         for (int i = 1; i < preparedStmtChildText.size(); i++)
         {
            sql.append(preparedStmtChildText.get(i));
            if (i < preparedStmtChildText.size() - 1)
               sql.append(", ");
         }
         sql.append(")");
      }
      else if ("if".equalsIgnoreCase(token))
      {
         if (db == null || db.isType("mysql"))
         {
            sql.append("IF(").append(preparedStmtChildText.get(0)).append(", ").append(preparedStmtChildText.get(1)).append(", ").append(preparedStmtChildText.get(2)).append(")");
         }
         else
         {
            sql.append("CASE WHEN ").append(preparedStmtChildText.get(0)).append(" THEN ").append(preparedStmtChildText.get(1)).append(" ELSE ").append(preparedStmtChildText.get(2)).append(" END");
         }
      }
      else if ("and".equalsIgnoreCase(token) || "or".equalsIgnoreCase(token))
      {
         sql.append("(");
         for (int i = 0; i < preparedStmtChildText.size(); i++)
         {
            sql.append(preparedStmtChildText.get(i).trim());
            if (i < preparedStmtChildText.size() - 1)
               sql.append(" ").append(token.toUpperCase()).append(" ");
         }
         sql.append(")");
      }
      else if ("not".equalsIgnoreCase(token))
      {
         sql.append("NOT (");
         for (int i = 0; i < preparedStmtChildText.size(); i++)
         {
            sql.append(preparedStmtChildText.get(i).trim());
            if (i < preparedStmtChildText.size() - 1)
               sql.append(" ");
         }
         sql.append(")");
      }
      else if ("count".equalsIgnoreCase(token))
      {
         //String s = "COUNT (1)";
         //sql.append(s);

         String pt = printTable() + "*";

         String acol = preparedStmtChildText.get(0);
         if (pt.equals(acol))
            acol = "*";

         String s = token.toUpperCase() + "(" + acol + ")";
         sql.append(s);

      }
      else if ("distinct".equalsIgnoreCase(token))
      {
         //TODO: what do you do with this as a function????
      }
      else if ("sum".equalsIgnoreCase(token) || "min".equalsIgnoreCase(token) || "max".equalsIgnoreCase(token))
      {
         String acol = preparedStmtChildText.get(0);
         String s = token.toUpperCase() + "(" + acol + ")";
         sql.append(s);
      }

      else if ("miles".equalsIgnoreCase(token))
      {
         sql.append("point(").append(preparedStmtChildText.get(0)).append(",").append(preparedStmtChildText.get(1)).append(") <@> point(").append(preparedStmtChildText.get(2)).append(",").append(preparedStmtChildText.get(3)).append(")");
      }
      else
      {
         throw new RuntimeException("Unable to parse: " + term);
      }

      return sql.toString();
   }

   protected String replace(Term parent, Term leaf, int index, String col, String val)
   {
      if (val == null || val.trim().equalsIgnoreCase("null"))
         return "NULL";

      if (parent.hasToken("if") && index > 0)
      {
         if (isNum(leaf))
            return val;
      }

      withColValue(col, val);
      return asVariableName(values.size() - 1);
   }

   protected String concatAll(String connector, String function, List strings)
   {
      StringBuffer buff = new StringBuffer((strings.size() > 1 ? "(" : ""));
      for (int i = 0; i < strings.size(); i++)
      {
         buff.append(strings.get(i)).append(function);
         if (i < strings.size() - 1)
            buff.append(connector);
      }
      if (strings.size() > 1)
         buff.append(")");

      return buff.toString();
   }

   protected boolean isCol(Term term)
   {
      if (!term.isLeaf())
         return false; //this is a function

      if (term.getQuote() == '"')//" is always the term identifier quote
         return true;

      if (term.getQuote() == '\'')
         return false; //this a string as specified by the user in the parsed rql

      if (isNum(term))
         return false;

      if (collection != null && collection.getProperty(term.getToken()) != null)
         return true;

      if (term.getParent() != null && term.getParent().indexOf(term) == 0)
         return true;

      return false;
   }

   public String quoteCol(String str)
   {
      StringBuffer buff = new StringBuffer();
      String[] parts = str.split("\\.");
      for (int i = 0; i < parts.length; i++)
      {
         if ("*".equals(parts[i]))
            buff.append(parts[i]);
         else
            buff.append(columnQuote).append(parts[i]).append(columnQuote);
         if (i < parts.length - 1)
            buff.append(".");
      }

      return buff.toString();//columnQuote + str + columnQuote;
   }

   public String printTable()
   {
      return quoteCol(collection.getTableName());
   }

   public String printCol(String columnName)
   {
      if (collection != null && columnName.indexOf(".") < 0)
         columnName = collection.getTableName() + "." + columnName;

      return quoteCol(columnName);
   }

   protected String asVariableName(int valuesPairIdx)
   {
      return "?";
   }

   protected String asString(String string)
   {
      return stringQuote + string + stringQuote;
   }

   protected String asString(Term term)
   {
      String token = term.token;
      Term parent = term.getParent();
      if (parent != null)
      {
         if (parent.hasToken("w") || parent.hasToken("wo"))
         {
            if (!token.startsWith("*"))
               token = "*" + token;

            if (!token.endsWith("*"))
               token += "*";
         }
         else if (parent.hasToken("sw") && !token.endsWith("*"))
         {
            token = token + "*";
         }
         else if (parent.hasToken("ew") && !token.startsWith("*"))
         {
            token = "*" + token;
         }

         if (parent.hasToken("eq", "ne", "w", "sw", "ew", "like", "wo"))
         {
            token = token.replace('*', '%');
         }

         boolean wildcard = token.indexOf('%') >= 0;
         if (wildcard)
         {
            // escape underscores because SQL pattern matching enables you to use "_" to match any single character and we don't want that behavior
            token = token.replace("_", "\\_");
         }

      }

      return stringQuote + token.toString() + stringQuote;
   }

   protected String asNum(String token)
   {
      if ("true".equalsIgnoreCase(token))
         return "1";

      if ("false".equalsIgnoreCase(token))
         return "0";

      return token;
   }

   protected boolean isNum(Term term)
   {
      if (!term.isLeaf() || term.isQuoted())
         return false;

      String token = term.getToken();
      try
      {
         Double.parseDouble(token);
         return true;
      }
      catch (Exception ex)
      {
         //not a number, ignore
      }

      if ("true".equalsIgnoreCase(token))
         return true;

      if ("false".equalsIgnoreCase(token))
         return true;

      if ("null".equalsIgnoreCase(token))
         return true;

      return false;
   }

   public class Parts
   {
      public String select = null;
      public String from   = null;
      public String where  = null;
      public String group  = null;
      public String order  = null;
      public String limit  = null;

      void withSql(String sql)
      {
         if (sql == null)
            return;

         sql = sql.trim();

         SqlTokenizer tok = new SqlTokenizer(sql);

         String clause = null;
         while ((clause = tok.nextClause()) != null)
         {
            String token = (clause.indexOf(" ") > 0 ? clause.substring(0, clause.indexOf(" ")) : clause).toLowerCase();

            switch (token)
            {
               //case "insert":
               //case "update":
               case "delete":
               case "select":
                  select = clause;
                  continue;
               case "from":
                  from = clause;
                  continue;
               case "where":
                  where = clause;
                  continue;
               case "group":
                  group = clause;
                  continue;
               case "order":
                  order = clause;
                  continue;
               case "limit":
                  limit = clause;
                  continue;
               default :
                  throw new RuntimeException("Unable to parse: \"" + clause + "\" - \"" + sql + "\"");
            }
         }
      }
   }

   public SqlQuery withSelectSql(String selectSql)
   {
      this.selectSql = selectSql;
      return this;
   }

   @Override
   public SqlQuery withDb(D db)
   {
      super.withDb(db);
      if (db.isType("mysql"))
         withColumnQuote('`');

      return this;
   }

   public SqlQuery withType(String type)
   {
      this.type = type;
      return this;
   }

   public String getType()
   {
      if (db != null)
         return db.getType();

      return type;
   }

   public void withStringQuote(char stringQuote)
   {
      this.stringQuote = stringQuote;
   }

   public void withColumnQuote(char columnQuote)
   {
      this.columnQuote = columnQuote;
   }

}
