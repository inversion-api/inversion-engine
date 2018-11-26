/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
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
package io.rcktapp.rql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import io.forty11.j.J;
import io.rcktapp.api.Table;

public class Stmt
{

   Rql                               rql          = null;

   public Parts                      parts        = null;
   Replacer                          replacer     = null;
   Table                             table        = null;

   public boolean                    restrictCols = false;

   //url arg order is undependable so order does not matter here either
   public HashMap<String, Predicate> cols         = new LinkedHashMap();

   public boolean                    distinct     = false;

   public List<Predicate>            where        = new ArrayList();
   public List<String>               groups       = new ArrayList();
   public List<Order>                order        = new ArrayList();

   public int                        pagenum      = -1;
   public int                        pagesize     = -1;

   public int                        limit        = -1;
   public int                        offset       = -1;

   public int                        maxRows      = 100;

   public String                     rowcount     = null;

   public Stmt(Rql rql, Parts parts, Replacer replacer, Table table)
   {
      this.rql = rql;

      this.parts = parts;
      this.replacer = replacer;
      this.table = table;
   }

   void addGroupBy(String col)
   {
      if (!groups.contains(col))
         groups.add(col);
   }

   void addCol(String name, Predicate predicate)
   {
      if (name == null)
         name = "$$$ANON" + UUID.randomUUID().toString();

      name = Parser.dequote(name);

      cols.put(name, predicate);
   }

   /**
    * The select clause will include at least the columns supplied 
    * in the order in which they are supplied here.  Functions such
    * as 'as()' can add addition columns to this list.  Such a column
    * if not included in the arg here would come after all of the 
    * cols supplied here in the select statement.
    * 
    * Also causes a SELECT * to be replace with the specific column
    * names, removeing the *.  Simply adding columns via "as(" would
    * add to the * not replace it.
    * 
    * @param ordered
    */
   void setCols(List<String> ordered)
   {
      restrictCols = true;

      LinkedHashMap<String, Predicate> newCols = new LinkedHashMap();
      for (String c : ordered)
      {
         c = Parser.dequote(c);
         newCols.put(c, null);
      }

      for (String c : cols.keySet())
      {
         c = Parser.dequote(c);
         newCols.put(c, cols.get(c));
      }

      cols = newCols;
   }

   public String toSql() throws Exception
   {
      if (this.limit < 0)
      {
         this.limit = maxRows;
      }
      if (this.pagesize < 0)
      {
         this.pagesize = this.limit;
      }
      if (this.pagenum < 0)
      {
         this.pagenum = (this.offset / this.pagesize) + 1;
      }

      if (this.cols.size() > 0)
      {
         String cols = "";

         for (String col : this.cols.keySet())
         {
            Predicate p = this.cols.get(col);
            if (p == null)
            {
               cols += " " + rql.parser.asCol(col) + ",";
            }
            else
            {
               if (col.indexOf("$$$ANON") > -1)
               {
                  cols += " " + print(table, p, replacer, null) + ",";
               }
               else
               {
                  cols += " " + print(table, p, replacer, null) + " AS " + rql.parser.asStr(col) + ",";
               }
            }
         }

         if (cols.length() > 0)
         {
            cols = cols.substring(0, cols.length() - 1) + " ";
         }

         if (this.restrictCols && parts.select.indexOf(" * ") > 0)
         {
            parts.select = parts.select.replaceFirst(" \\* ", cols);
         }
         else
         {
            parts.select = parts.select.trim() + ", " + cols;
         }
      }

      if (this.distinct && parts.select.toLowerCase().indexOf("distinct") < 0)
      {
         int idx = parts.select.toLowerCase().indexOf("select") + 6;
         parts.select = parts.select.substring(0, idx) + " DISTINCT " + parts.select.substring(idx, parts.select.length());
      }

      if (rql.isCalcRowsFound() && this.pagenum > 0 && parts.select.toLowerCase().trim().startsWith("select"))
      {
         int idx = parts.select.toLowerCase().indexOf("select") + 6;
         parts.select = parts.select.substring(0, idx) + " SQL_CALC_FOUND_ROWS " + parts.select.substring(idx, parts.select.length());
      }

      //--WHERE 

      if (this.where.size() > 0)
      {
         //         if (parts.where == null)
         //            parts.where = " WHERE ";
         //         else
         //            parts.where += " AND ";

         for (int i = 0; i < this.where.size(); i++)
         {
            Predicate p = this.where.get(i);

            String where = print(table, p, replacer, null);
            if (where != null)
            {
               if (J.empty(parts.where))
                  parts.where = " WHERE " + where;
               else
                  parts.where += " AND " + where;
            }
         }
      }

      //--GROUP BY
      if (this.groups.size() > 0)
      {
         if (parts.group == null)
            parts.group = "GROUP BY ";

         for (String group : this.groups)
         {
            if (!parts.group.endsWith("GROUP BY "))
               parts.group += ", ";
            parts.group += rql.parser.asCol(group);
         }
      }

      //-- now setup the "ORDER BY" clause based on the
      //-- "sort" parameter.  Multiple sorts can be 
      //-- comma separated and a leading '-' indicates
      //-- descending sort for that field
      //--
      //-- ex: "sort=firstName,-age"

      if (this.order.size() > 0)
      {
         if (parts.order == null)
            parts.order = "ORDER BY ";

         for (Order order : this.order)
         {
            if (!parts.order.endsWith("ORDER BY "))
               parts.order += ", ";

            parts.order += order.col + " " + order.dir;
         }
      }

      //-- now setup the LIMIT clause based
      //-- off of the  "offset" and "limit"
      //-- params OR the "page" and "pageSize"
      //-- query params.  

      if (this.pagenum >= 0 && this.pagesize >= 0)
      {
         if (offset <= 0)
            this.offset = (this.pagenum - 1) * this.pagesize;

         this.limit = this.pagesize;
      }

      //if (this.limit < 0 && maxResults > 0)
      //   this.limit = maxResults;

      parts.limit = this.buildLimitClause();

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
         if (table.getCol(rql.parser.dequote(col)) == null)
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
         if (rql.parser.isLiteral(val) && r != null)
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
         String s = token.toUpperCase() + "(" + rql.parser.swapIf(acol, '\'', '`') + ")";
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

   String buildLimitClause()
   {
      String s = null;
      if (this.limit >= 0 || this.offset >= 0)
      {
         if ("postgres".equalsIgnoreCase(rql.getType()) || "redshift".equalsIgnoreCase(rql.getType()))
         {
            s = "";
            if (this.offset >= 0)
               s += "OFFSET " + this.offset;

            if (this.limit >= 0)
            {
               s += " LIMIT " + this.limit;
            }
         }
         else
         {
            s = "LIMIT ";
            if (this.offset >= 0)
               s += this.offset;

            if (this.limit >= 0)
            {
               if (!s.endsWith("LIMIT "))
                  s += ", ";

               s += this.limit;
            }
         }
      }
      return s;
   }

   public int getMaxRows()
   {
      return maxRows;
   }

   public void setMaxRows(int maxRows)
   {
      this.maxRows = maxRows;
   }

}