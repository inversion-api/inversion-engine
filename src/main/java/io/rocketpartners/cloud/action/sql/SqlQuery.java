/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * http://rocketpartners.io
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.rocketpartners.cloud.action.sql;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import io.rocketpartners.cloud.model.Column;
import io.rocketpartners.cloud.model.Index;
import io.rocketpartners.cloud.model.Results;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.rql.Group;
import io.rocketpartners.cloud.rql.Order;
import io.rocketpartners.cloud.rql.Order.Sort;
import io.rocketpartners.cloud.rql.Page;
import io.rocketpartners.cloud.rql.Query;
import io.rocketpartners.cloud.rql.Select;
import io.rocketpartners.cloud.rql.Term;
import io.rocketpartners.cloud.rql.Where;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.utils.Rows;
import io.rocketpartners.cloud.utils.Rows.Row;
import io.rocketpartners.cloud.utils.SqlUtils;

public class SqlQuery extends Query<SqlQuery, SqlDb, Table, Select<Select<Select, SqlQuery>, SqlQuery>, Where<Where<Where, SqlQuery>, SqlQuery>, Group<Group<Group, SqlQuery>, SqlQuery>, Order<Order<Order, SqlQuery>, SqlQuery>, Page<Page<Page, SqlQuery>, SqlQuery>>
{
   protected char stringQuote = '\'';
   protected char columnQuote = '"';

   String         selectSql   = null;

   String         type        = null;

   public SqlQuery(Table table, List<Term> terms)
   {
      super(table, terms);
   }

   protected boolean addTerm(String token, Term term)
   {
      if (term.hasToken("eq"))
      {
         //ignore extraneous name=value pairs if 'name' is not a column
         if (table != null && table.getColumn(term.getToken(0)) == null)
            return true;
      }
      return super.addTerm(token, term);
   }

   protected Results<Row> doSelect() throws Exception
   {
      SqlDb db = getDb();
      Connection conn = db.getConnection();
      String sql = getPreparedStmt();

      List values = getColValues();
      Rows rows = SqlUtils.selectRows(conn, sql, values);
      //Rows rows = SqlUtils.selectRows(conn, "SELECT * FROM ORDERS");
      int foundRows = -1;

      if (Chain.peek().get("foundRows") == null)
      {
         if (rows.size() == 0)
         {
            foundRows = 0;
         }
         else if (db.isType("mysql"))
         {
            sql = "SELECT FOUND_ROWS()";
            foundRows = SqlUtils.selectInt(conn, sql);
         }
         else
         {
            sql = "SELECT * " + sql.substring(sql.indexOf("FROM "), sql.length());

            if (sql.indexOf("LIMIT ") > 0)
               sql = sql.substring(0, sql.indexOf("LIMIT "));

            if (sql.indexOf("OFFSET ") > 0)
               sql = sql.substring(0, sql.indexOf("OFFSET "));

            if (sql.indexOf("ORDER BY ") > 0)
               sql = sql.substring(0, sql.indexOf("ORDER BY "));

            sql = "SELECT count(1) FROM ( " + sql + " ) as q";

            foundRows = SqlUtils.selectInt(conn, sql, getColValues());
         }

      }

      return new Results(this, foundRows, rows);
   }

   @Override
   public SqlQuery withDb(SqlDb db)
   {
      super.withDb(db);
      if (db.isType("mysql"))
         withColumnQuote('`');

      return this;
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

      Parts parts = new Parts(selectSql);

      StringBuffer cols = new StringBuffer();

      List<Term> terms = select().columns();
      for (int i = 0; i < terms.size(); i++)
      {
         Term term = terms.get(i);
         if (term.hasToken("as"))
         {
            Term function = term.getTerm(0);
            cols.append(" ").append(print(function, null, preparedStmt));

            String colName = term.getToken(1);
            if (!(empty(colName) || colName.indexOf("$$$ANON") > -1))
            {
               cols.append(" AS " + asString(colName));
            }
         }
         else if (term.getToken().indexOf(".") < 0)
         {
            cols.append(" " + asCol(term.getToken()));
         }

         if (i < terms.size() - 1)
            cols.append(", ");
      }

      if (cols.length() > 0)
      {
         boolean restrictCols = find("includes") != null;
         int star = parts.select.indexOf(" * ");
         if (restrictCols && star > 0)
         {
            //force the inclusion of pk cols even if there
            //were not requested by the caller.  Actions such
            //as RestGetHandler need the pk values to do 
            //anything interesting with the results and they
            //are responsible for filtering out the values
            //
            //TODO: maybe this should be put into Select.columns()
            //so all query subclasses will inherit the behavior???
            if (table() != null)
            {
               Index primaryIndex = table().getPrimaryIndex();
               if (primaryIndex != null)
               {
                  for (Column pkCol : primaryIndex.getColumns())
                  {
                     if (cols.indexOf(asCol(pkCol.getName())) < 0)
                        cols.append(", ").append(asCol(pkCol.getName()));
                  }
               }
            }

            parts.select = parts.select.substring(0, star + 1) + cols + parts.select.substring(star + 2, parts.select.length());
         }
         else
         {
            parts.select = parts.select.trim() + ", " + cols;
         }
      }

      if (select().isDistinct() && parts.select.toLowerCase().indexOf("distinct") < 0)
      {
         int idx = parts.select.toLowerCase().indexOf("select") + 6;
         parts.select = parts.select.substring(0, idx) + " DISTINCT " + parts.select.substring(idx, parts.select.length());
      }

      if (Chain.peek() != null && Chain.peek().get("foundRows") == null && "mysql".equalsIgnoreCase(getType()) && parts.select.toLowerCase().trim().startsWith("select"))
      {
         int idx = parts.select.toLowerCase().indexOf("select") + 6;
         parts.select = parts.select.substring(0, idx) + " SQL_CALC_FOUND_ROWS " + parts.select.substring(idx, parts.select.length());
      }

      //            if (isCalcRowsFound() && stmt.pagenum > 0 && stmt.parts.select.toLowerCase().trim().startsWith("select"))
      //            {
      //               
      //            }

      terms = where().filters();
      for (int i = 0; i < terms.size(); i++)
      {
         Term term = terms.get(i);

         String where = print(term, null, preparedStmt);
         if (where != null)
         {
            if (empty(parts.where))
               parts.where = " WHERE " + where;
            else
               parts.where += " AND " + where;
         }
      }

      Term groupBy = find("group");
      if (groupBy != null)
      {
         if (parts.group == null)
            parts.group = "GROUP BY ";

         for (Term group : groupBy.getTerms())
         {
            if (!parts.group.endsWith("GROUP BY "))
               parts.group += ", ";
            parts.group += asCol(group.getToken());
         }
      }

      List<Sort> sorts = order().getSorts();
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

         parts.order += asCol(sort.getProperty()) + (sort.isAsc() ? " ASC" : " DESC");
      }

      //-- now setup the LIMIT clause based
      //-- off of the  "offset" and "limit"
      //-- params OR the "page" and "pageSize"
      //-- query params.  

      int offset = page().getOffset();
      int limit = page().getLimit();

      parts.limit = this.buildLimitClause(offset, limit);

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

   protected String buildLimitClause(int offset, int limit)
   {
      String s = null;
      if (limit >= 0 || offset >= 0)
      {
         if (db == null || db().isType("mysql"))
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
         else
         {
            s = "";
            if (offset >= 0)
               s += "OFFSET " + offset;

            if (limit >= 0)
            {
               s += " LIMIT " + limit;
            }
         }
      }
      return s;
   }

   public String replace(Term parent, Term leaf, int index, String col, String val)
   {
      if (val == null || val.trim().equalsIgnoreCase("null"))
         return "NULL";

      if (parent.hasToken("if") && index > 0)
      {
         if (SqlQuery.isNum(leaf))
            return val;
      }

      withColValue(col, val);
      return "?";
   }

   public static String dequote(String str)
   {
      char[] quoteChars = new char[]{'\'', '"', '`'};
      if (str == null)
         return null;

      while (str.length() >= 2 && str.charAt(0) == str.charAt(str.length() - 1))// && (str.charAt(0) == '\'' || str.charAt(0) == '"' || str.charAt(0) == '`'))
      {
         boolean changed = false;
         for (int i = 0; i < quoteChars.length; i++)
         {
            if (str.charAt(0) == quoteChars[i])
            {
               str = str.substring(1, str.length() - 1);
               changed = true;
               break;
            }
         }
         if (!changed)
            break;
      }

      return str;
   }

   protected String print(Term term, String col, boolean preparedStmt)
   {
      if (term.isLeaf())
      {
         String token = term.getToken();

         String value = null;
         if (isCol(term))
         {
            value = asCol(token);
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

      StringBuffer sql = new StringBuffer("");

      List<Term> terms = term.getTerms();
      String token = term.getToken();

      for (int i = 0; i < term.size(); i++)
      {
         Term child = term.getTerm(i);
         if (isCol(child))
         {
            col = child.token;
            break;
         }
      }

      List<String> strings = new ArrayList();
      for (Term t : terms)
      {
         strings.add(print(t, col, preparedStmt));
      }

      List<String> origionals = new ArrayList(strings);

      //allows for callers to substitute callable statement "?"s
      //and/or to account for data type conversion 
      if (preparedStmt)
      {
         for (int i = 0; i < terms.size(); i++)
         {
            Term t = terms.get(i);
            if (t.isLeaf())
            {
               String val = strings.get(i);
               if (val.charAt(0) != columnQuote)
               {
                  val = dequote(val);//t.getToken();//go back to the unprinted/quoted version
                  strings.set(i, replace(term, t, i, col, val));
               }
            }
         }
      }

      if (term.hasToken("eq", "ne", "like", "w", "sw", "ew", "wo"))
      {
         if (terms.size() > 2)
            sql.append("(");

         String string0 = strings.get(0);

         for (int i = 1; i < terms.size(); i++)
         {
            String stringI = strings.get(i);
            if ("null".equalsIgnoreCase(stringI))
            {
               if (term.hasToken("eq", "like", "w", "sw", "ew", "wo"))
               {
                  sql.append(string0).append(" IS NULL ");
               }
               else
               {
                  sql.append(string0).append(" IS NOT NULL ");
               }
            }
            else
            {
               boolean wildcard = origionals.get(i).indexOf('%') >= 0;

               if (wildcard)
               {
                  if (term.hasToken("ne") || term.hasToken("wo"))
                     sql.append(string0).append(" NOT LIKE ").append(stringI);
                  else
                     sql.append(string0).append(" LIKE ").append(stringI);
               }
               else
               {
                  if (term.hasToken("eq"))
                     sql.append(string0).append(" = ").append(stringI);
                  else
                     sql.append(" NOT ").append(string0).append(" <=> ").append(stringI);
               }
            }

            if (i < terms.size() - 1)
               sql.append(" OR ").append(string0);
         }

         if (terms.size() > 2)
            sql.append(")");
      }
      else if ("nn".equalsIgnoreCase(token))
      {
         sql.append(concatAll(" AND ", " IS NOT NULL", strings));
      }
      else if ("n".equalsIgnoreCase(token))
      {
         sql.append(concatAll(" AND ", " IS NULL", strings));
      }
      else if ("lt".equalsIgnoreCase(token))
      {
         sql.append(strings.get(0)).append(" < ").append(strings.get(1));
      }
      else if ("le".equalsIgnoreCase(token))
      {
         sql.append(strings.get(0)).append(" <= ").append(strings.get(1));
      }
      else if ("gt".equalsIgnoreCase(token))
      {
         sql.append(strings.get(0)).append(" > ").append(strings.get(1));
      }
      else if ("ge".equalsIgnoreCase(token))
      {
         sql.append(strings.get(0)).append(" >= ").append(strings.get(1));
      }
      else if ("in".equalsIgnoreCase(token) || "out".equalsIgnoreCase(token))
      {
         sql.append(strings.get(0));

         if ("out".equalsIgnoreCase(token))
            sql.append(" NOT");

         sql.append(" IN(");
         for (int i = 1; i < strings.size(); i++)
         {
            sql.append(strings.get(i));
            if (i < strings.size() - 1)
               sql.append(", ");
         }
         sql.append(")");
      }
      else if ("if".equalsIgnoreCase(token))
      {
         sql.append("IF(").append(strings.get(0)).append(", ").append(strings.get(1)).append(", ").append(strings.get(2)).append(")");
      }
      else if ("and".equalsIgnoreCase(token) || "or".equalsIgnoreCase(token))
      {
         sql.append("(");
         for (int i = 0; i < strings.size(); i++)
         {
            sql.append(strings.get(i).trim());
            if (i < strings.size() - 1)
               sql.append(" ").append(token.toUpperCase()).append(" ");
         }
         sql.append(")");
      }
      else if ("sum".equalsIgnoreCase(token) || "count".equalsIgnoreCase(token) || "min".equalsIgnoreCase(token) || "max".equalsIgnoreCase(token) || "distinct".equalsIgnoreCase(token))
      {
         String acol = strings.get(0);
         String s = token.toUpperCase() + "(" + acol + ")";
         sql.append(s);
      }

      else if ("miles".equalsIgnoreCase(token))
      {
         sql.append("point(").append(strings.get(0)).append(",").append(strings.get(1)).append(") <@> point(").append(strings.get(2)).append(",").append(strings.get(3)).append(")");
      }
      else
      {
         throw new RuntimeException("Unable to parse: " + term);
      }

      return sql.toString();
   }

   public SqlQuery withSelectSql(String selectSql)
   {
      this.selectSql = selectSql;
      return this;
   }

   public String concatAll(String connector, String function, List strings)
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

      if (table != null && table.getColumn(term.getToken()) != null)
         return true;

      if (term.getParent() != null && term.getParent().indexOf(term) == 0)
         return true;

      return false;
   }

   public String quoteCol(String str)
   {
      return columnQuote + str + columnQuote;
   }

   public String asCol(String columnName)
   {
      return quoteCol(columnName);
   }

   public String asString(String string)
   {
      return stringQuote + string + stringQuote;
   }

   public String asString(Term term)
   {
      String token = term.token;
      Term parent = term.getParent();
      if (parent != null)
      {
         if (parent.hasToken("w") || parent.hasToken("wo"))
         {
            token = "*" + token + "*";
         }
         else if (parent.hasToken("sw"))
         {
            token = token + "*";
         }
         else if (parent.hasToken("ew"))
         {
            token = "*" + token;
         }

         if (parent.hasToken("eq", "ne", "w", "sw", "ew", "like", "wo"))
         {
            token = token.replace('*', '%');
         }
      }

      return stringQuote + token.toString() + stringQuote;
   }

   protected static String asNum(String token)
   {
      if ("true".equalsIgnoreCase(token))
         return "1";

      if ("false".equalsIgnoreCase(token))
         return "0";

      return token;
   }

   protected static boolean isNum(Term term)
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

      public Parts(String sql)
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

         //         select = chopFirst(sql, "select", "from");
         //
         //         if (empty(select))
         //            select = chopFirst(sql, "update", "where");
         //
         //         if (empty(select))
         //            select = chopFirst(sql, "delete", "from");
         //
         //         if (select != null)
         //         {
         //            sql = sql.substring(select.length(), sql.length());
         //
         //            if (sql.trim().substring(4).trim().startsWith("("))
         //            {
         //               int end = sql.lastIndexOf("as") + 3;
         //               String rest = sql.substring(end, sql.length());
         //               int[] otherIdx = findFirstOfFirstOccurances(rest, "where", " group by", "order", "limit");
         //               if (otherIdx != null)
         //               {
         //                  end += otherIdx[0];
         //               }
         //               else
         //               {
         //                  end = sql.length();
         //               }
         //
         //               from = sql.substring(0, end);
         //               sql = sql.substring(end, sql.length());
         //            }
         //            else
         //            {
         //               from = chopLast(sql, "from", "where", "group by", "order", "limit");
         //            }
         //            where = chopLast(sql, "where", "group by", "order", "limit");
         //            group = chopLast(sql, "group by", "order", "limit");
         //            order = chopLast(sql, "order", "limit");
         //            limit = chopLast(sql, "limit");
         //         }
      }
   }
}
