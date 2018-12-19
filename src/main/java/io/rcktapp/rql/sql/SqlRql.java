package io.rcktapp.rql.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import io.forty11.j.J;
import io.rcktapp.api.Table;
import io.rcktapp.rql.Order;
import io.rcktapp.rql.Predicate;
import io.rcktapp.rql.Replacer;
import io.rcktapp.rql.Rql;
import io.rcktapp.rql.Stmt;

public class SqlRql extends Rql
{
   public static final HashSet<String> RESERVED = new HashSet(Arrays.asList(new String[]{"as", "includes", "sort", "order", "offset", "limit", "distinct", "aggregate", "function", "sum", "count", "min", "max"}));

   static
   {
      Rql.addRql(new SqlRql("mysql"));
      Rql.addRql(new SqlRql("postgresql"));
      Rql.addRql(new SqlRql("postgres"));
      Rql.addRql(new SqlRql("redshift"));
   }

   private SqlRql(String type)
   {
      super(type);

      if (type != null && type.toLowerCase().indexOf("mysql") > -1)
      {
         setIdentifierQuote('`');
         setCalcRowsFound(true);
      }

      for (String reserved : RESERVED)
         addReserved(reserved);
   }

   public String toSql(Stmt stmt) throws Exception
   {
      if (stmt.limit < 0)
      {
         stmt.limit = stmt.maxRows;
      }
      if (stmt.pagesize < 0)
      {
         stmt.pagesize = stmt.limit;
      }
      if (stmt.pagenum < 0)
      {
         stmt.pagenum = (stmt.offset / stmt.pagesize) + 1;
      }

      if (stmt.cols.size() > 0)
      {
         String cols = "";

         for (String col : stmt.cols.keySet())
         {
            Predicate p = stmt.cols.get(col);
            if (p == null)
            {
               cols += " " + parser.asCol(col) + ",";
            }
            else
            {
               if (col.indexOf("$$$ANON") > -1)
               {
                  cols += " " + print(stmt.table, p, stmt.replacer, null) + ",";
               }
               else
               {
                  cols += " " + print(stmt.table, p, stmt.replacer, null) + " AS " + parser.asStr(col) + ",";
               }
            }
         }

         if (cols.length() > 0)
         {
            cols = cols.substring(0, cols.length() - 1) + " ";
         }

         if (stmt.restrictCols && stmt.parts.select.indexOf(" * ") > 0)
         {
            stmt.parts.select = stmt.parts.select.replaceFirst(" \\* ", cols);
         }
         else
         {
            stmt.parts.select = stmt.parts.select.trim() + ", " + cols;
         }
      }

      if (stmt.distinct && stmt.parts.select.toLowerCase().indexOf("distinct") < 0)
      {
         int idx = stmt.parts.select.toLowerCase().indexOf("select") + 6;
         stmt.parts.select = stmt.parts.select.substring(0, idx) + " DISTINCT " + stmt.parts.select.substring(idx, stmt.parts.select.length());
      }

      if (isCalcRowsFound() && stmt.pagenum > 0 && stmt.parts.select.toLowerCase().trim().startsWith("select"))
      {
         int idx = stmt.parts.select.toLowerCase().indexOf("select") + 6;
         stmt.parts.select = stmt.parts.select.substring(0, idx) + " SQL_CALC_FOUND_ROWS " + stmt.parts.select.substring(idx, stmt.parts.select.length());
      }

      //--WHERE 

      if (stmt.where.size() > 0)
      {
         //         if (parts.where == null)
         //            parts.where = " WHERE ";
         //         else
         //            parts.where += " AND ";

         for (int i = 0; i < stmt.where.size(); i++)
         {
            Predicate p = stmt.where.get(i);

            String where = print(stmt.table, p, stmt.replacer, null);
            if (where != null)
            {
               if (J.empty(stmt.parts.where))
                  stmt.parts.where = " WHERE " + where;
               else
                  stmt.parts.where += " AND " + where;
            }
         }
      }

      //--GROUP BY
      if (stmt.groups.size() > 0)
      {
         if (stmt.parts.group == null)
            stmt.parts.group = "GROUP BY ";

         for (String group : stmt.groups)
         {
            if (!stmt.parts.group.endsWith("GROUP BY "))
               stmt.parts.group += ", ";
            stmt.parts.group += parser.asCol(group);
         }
      }

      //-- now setup the "ORDER BY" clause based on the
      //-- "sort" parameter.  Multiple sorts can be 
      //-- comma separated and a leading '-' indicates
      //-- descending sort for that field
      //--
      //-- ex: "sort=firstName,-age"

      if (stmt.order.size() > 0)
      {
         if (stmt.parts.order == null)
            stmt.parts.order = "ORDER BY ";

         for (Order order : stmt.order)
         {
            if (!stmt.parts.order.endsWith("ORDER BY "))
               stmt.parts.order += ", ";

            stmt.parts.order += order.col + " " + order.dir;
         }
      }

      //-- now setup the LIMIT clause based
      //-- off of the  "offset" and "limit"
      //-- params OR the "page" and "pageSize"
      //-- query params.  

      if (stmt.pagenum >= 0 && stmt.pagesize >= 0)
      {
         if (stmt.offset <= 0)
            stmt.offset = (stmt.pagenum - 1) * stmt.pagesize;

         stmt.limit = stmt.pagesize;
      }

      //if (this.limit < 0 && maxResults > 0)
      //   this.limit = maxResults;

      stmt.parts.limit = this.buildLimitClause(stmt);

      //--compose the final statement
      String buff = stmt.parts.select;

      buff += " \r\n" + stmt.parts.from;

      if (stmt.parts.where != null)
         buff += " \r\n" + stmt.parts.where;

      if (stmt.parts.select.toLowerCase().startsWith("select "))
      {
         if (stmt.parts.group != null)
            buff += " \r\n" + stmt.parts.group;

         if (stmt.parts.order != null)
            buff += " \r\n" + stmt.parts.order;

         if (stmt.parts.limit != null)
            buff += " \r\n" + stmt.parts.limit;
      }

      return buff.toString();
   }

   String print(Table table, Predicate p, Replacer r, String col) throws Exception
   {
      //System.out.println("processPredicate(" + p + ")");
      StringBuffer sql = new StringBuffer("");
      String token = p.token;
      if (token.endsWith("("))
         token = token.substring(0, token.length() - 1);

      //System.out.println(token);

      if (p.terms.size() > 0 && p.terms.get(0).terms.size() == 0)
      {
         String acol = p.terms.get(0).token;
         if (!acol.startsWith("\'"))
            col = acol;
      }

      if (col != null && table != null)
      {
         if (table.getColumn(parser.dequote(col)) == null)
            return null;
      }

      List<String> terms = new ArrayList();
      for (int i = 0; i < p.terms.size(); i++)
      {
         terms.add(print(table, p.terms.get(i), r, col));
      }

      List<String> preReplace = new ArrayList(terms);

      //allows for callers to substitute callable statement "?"s
      //and/or to account for data type conversion 
      for (int i = 0; col != null && i < p.terms.size(); i++)
      {
         String val = terms.get(i);
         if (parser.isLiteral(val) && r != null)
         {
            if ("w".equalsIgnoreCase(token))
            {
               val = "'*" + val.replace("'", "") + "*'";
            }
            else if ("sw".equalsIgnoreCase(token))
            {
               val = "'" + val.replace("'", "") + "*'";
            }
            else if ("ew".equalsIgnoreCase(token))
            {
               val = "'*" + val.replace("'", "") + "'";
            }

            terms.set(i, r.replace(p, col, val));
         }
      }

      if ("eq".equalsIgnoreCase(token))
      {
         String term1 = terms.get(0);
         String term2 = terms.get(1);
         String orig2 = preReplace.get(1);

         if ("null".equalsIgnoreCase(term2))
         {
            sql.append(term1).append(" IS NULL ");
         }
         else if (orig2.indexOf('*') > -1)
         {
            term2 = term2.replace('*', '%');
            sql.append(term1).append(" LIKE ").append(term2);
         }
         else
         {
            sql.append(term1).append(" = ").append(term2);
         }
      }
      else if ("w".equalsIgnoreCase(token) || "sw".equalsIgnoreCase(token) || "ew".equalsIgnoreCase(token))
      {
         String term1 = terms.get(0);
         for (int i = 1; i < terms.size(); i++)
         {
            if (i == 1)
               sql.append(term1).append(" LIKE ").append(terms.get(i));
            else
               sql.append(" OR ").append(term1).append(" LIKE ").append(terms.get(i));
         }
      }
      else if ("ne".equalsIgnoreCase(token))
      {
         String term1 = terms.get(0);
         String term2 = terms.get(1);
         String orig2 = preReplace.get(1);

         if ("null".equalsIgnoreCase(term2))
         {
            sql.append(term1).append(" IS NOT NULL ");
         }
         else if (orig2.indexOf('*') > -1)
         {
            term2 = term2.replace('*', '%');
            sql.append(term1).append(" NOT LIKE ").append(term2);
         }
         else
         {
            // using a null safe equals with a NOT to include results where the db has a null value for this column
            // the <> operator will only include non-null values that are not equal to the passed in term
            sql.append(" NOT ").append(term1).append(" <=> ").append(term2);
         }
      }
      else if ("nn".equalsIgnoreCase(token))
      {
         String term1 = terms.get(0);

         sql.append(term1).append(" IS NOT NULL ");
      }
      else if ("n".equalsIgnoreCase(token))
      {
         String term1 = terms.get(0);

         sql.append(term1).append(" IS NULL ");
      }
      else if ("lt".equalsIgnoreCase(token))
      {
         sql.append(terms.get(0)).append(" < ").append(terms.get(1));
      }
      else if ("le".equalsIgnoreCase(token))
      {
         sql.append(terms.get(0)).append(" <= ").append(terms.get(1));
      }
      else if ("gt".equalsIgnoreCase(token))
      {
         sql.append(terms.get(0)).append(" > ").append(terms.get(1));
      }
      else if ("ge".equalsIgnoreCase(token))
      {
         sql.append(terms.get(0)).append(" >= ").append(terms.get(1));
      }
      else if ("in".equalsIgnoreCase(token) || "out".equalsIgnoreCase(token))
      {
         sql.append(terms.get(0));

         if ("out".equalsIgnoreCase(token))
            sql.append(" NOT");

         sql.append(" IN(");
         for (int i = 1; i < terms.size(); i++)
         {
            sql.append(terms.get(i));
            if (i < terms.size() - 1)
               sql.append(", ");
         }
         sql.append(")");
      }
      else if ("if".equalsIgnoreCase(token))
      {
         sql.append("IF(").append(terms.get(0)).append(", ").append(terms.get(1)).append(", ").append(terms.get(2)).append(")");
      }
      else if ("and".equalsIgnoreCase(token) || "or".equalsIgnoreCase(token))
      {
         sql.append("(");
         for (int i = 0; i < terms.size(); i++)
         {
            sql.append(terms.get(i).trim());
            if (i < terms.size() - 1)
               sql.append(" ").append(token.toUpperCase()).append(" ");
         }
         sql.append(")");
      }
      else if ("sum".equalsIgnoreCase(token) || "count".equalsIgnoreCase(token) || "min".equalsIgnoreCase(token) || "max".equalsIgnoreCase(token) || "distinct".equalsIgnoreCase(token))
      {
         String acol = terms.get(0);
         String s = token.toUpperCase() + "(" + parser.swapIf(acol, '\'', '`') + ")";
         sql.append(s);

      }
      else if ("miles".equalsIgnoreCase(token))
      {
         sql.append("point(").append(terms.get(0)).append(",").append(terms.get(1)).append(") <@> point(").append(terms.get(2)).append(",").append(terms.get(3)).append(")");
      }
      else if (terms.size() == 0)
      {
         sql.append(token);
      }
      else
      {
         throw new RuntimeException("Unable to parse: " + p);
      }

      return sql.toString();
   }

   String buildLimitClause(Stmt stmt)
   {
      String s = null;
      if (stmt.limit >= 0 || stmt.offset >= 0)
      {
         if ("postgres".equalsIgnoreCase(getType()) || "redshift".equalsIgnoreCase(getType()))
         {
            s = "";
            if (stmt.offset >= 0)
               s += "OFFSET " + stmt.offset;

            if (stmt.limit >= 0)
            {
               s += " LIMIT " + stmt.limit;
            }
         }
         else
         {
            s = "LIMIT ";
            if (stmt.offset >= 0)
               s += stmt.offset;

            if (stmt.limit >= 0)
            {
               if (!s.endsWith("LIMIT "))
                  s += ", ";

               s += stmt.limit;
            }
         }
      }
      return s;
   }
}
