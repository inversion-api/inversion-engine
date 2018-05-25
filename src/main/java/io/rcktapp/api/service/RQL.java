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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.rcktapp.api.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.forty11.j.J;
import io.forty11.sql.Sql;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.SC;

public class RQL
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

   static HashSet RESERVED_NAMES = new HashSet(                                                                                                         //
                                                Arrays.asList(                                                                                          //
                                                      new String[]{"q", "filter", "expands", "excludes", "format", "replace", "ignores"}                //
                                                ));

   static HashSet FUNCTIONS      = new HashSet(                                                                                                         //
                                                Arrays.asList(                                                                                          //
                                                      new String[]{                                                                                     //
                                                            "(",                                                                                        //
                                                            "eq", "ne", "lt", "le", "gt", "ge",                                                         //
                                                            "and", "or",                                                                                //
                                                            "in", "out",                                                                                //
                                                            "group", "count", "sum", "min", "max", "aggregate", "function", "countascol", "rowcount",   //
                                                            "if", "as",                                                                                 //
                                                            "includes", "sort", "order", "offset", "limit",                                             //
                                                            "page", "pagenum", "pagesize", "distinct"}                                                  //
                                                ));

   static HashSet OPERATORS      = new HashSet(                                                                                                         //
                                                Arrays.asList(                                                                                          //
                                                      new String[]{"=", "eq", "ne", "lt", "le", "gt", "ge"}));

   static HashSet WHERE_OPS      = new HashSet(Arrays.asList(new String[]{"eq", "ne", "lt", "le", "gt", "ge", "in", "out", "if", "or", "and"}));

   public static Stmt toSql(String sqlStr, Map<String, String> params) throws Exception
   {
      return toSql(sqlStr, params, null);
   }

   public static Stmt toSql(String sqlStr, Map<String, String> params, Replacer r) throws Exception
   {
      String ignoresStr = params.get("ignores");

      if (!J.empty(ignoresStr))
      {
         List ignores = J.explode(ignoresStr.toLowerCase(), ",");
         for (String key : ((Map<String, String>) new HashMap(params)).keySet())
         {
            if (ignores.contains(key.toLowerCase()))
               params.remove(key);
         }
      }

      //-- do a search and replace on ${} vars in the passed in sql
      //-- before anything else is done.  Once replaced, these
      //-- vars are not considred by RQL
      {
         StringBuffer buff = new StringBuffer("");
         Pattern p = Pattern.compile("\\$\\{([^\\}]*)\\}");
         Matcher m = p.matcher(sqlStr);
         Set<String> toRemove = new HashSet();
         while (m.find())
         {
            String key = m.group(1);
            String value = Sql.check(params.get(key));
            if (value == null)
               throw new ApiException(SC.SC_400_BAD_REQUEST, "The value for param \"" + key + "\" can not be empty.");

            toRemove.add(key);
            m.appendReplacement(buff, value);
         }
         m.appendTail(buff);
         sqlStr = buff.toString();

         for (String key : toRemove)
         {
            params.remove(key);
         }
      }
      //--
      //-- end ${} search/replace

      Parts sql = new Parts(sqlStr);
      Stmt stmt = buildStmt(params, r);

      if (stmt.cols.size() > 0)
      {
         String cols = "";

         for (String col : stmt.cols.keySet())
         {
            Predicate p = stmt.cols.get(col);
            if (p == null)
            {
               cols += " " + asCol(col) + ",";
            }
            else
            {
               if (col.indexOf("$$$ANON") > -1)
               {
                  cols += " " + print(p, r, null) + ",";
               }
               else
               {
                  cols += " " + print(p, r, null) + " AS " + asStr(col) + ",";
               }
            }
         }

         if (cols.length() > 0)
         {
            cols = cols.substring(0, cols.length() - 1) + " ";
         }

         if (stmt.restrictCols && sql.select.indexOf(" * ") > 0)
         {
            sql.select = sql.select.replaceFirst(" \\* ", cols);
         }
         else
         {
            sql.select = sql.select.trim() + ", " + cols;
         }
      }

      if (stmt.distinct && sql.select.toLowerCase().indexOf("distinct") < 0)
      {
         int idx = sql.select.toLowerCase().indexOf("select") + 6;
         sql.select = sql.select.substring(0, idx) + " DISTINCT " + sql.select.substring(idx, sql.select.length());
      }

      if (stmt.pagenum > 0)
      {
         int idx = sql.select.toLowerCase().indexOf("select") + 6;
         sql.select = sql.select.substring(0, idx) + " SQL_CALC_FOUND_ROWS " + sql.select.substring(idx, sql.select.length());
      }

      //--WHERE 

      if (stmt.where.size() > 0)
      {
         if (sql.where == null)
            sql.where = " WHERE ";
         else
            sql.where += " AND ";

         for (int i = 0; i < stmt.where.size(); i++)
         {
            Predicate p = stmt.where.get(i);
            if (i > 0)
               sql.where += " AND ";

            sql.where += print(p, r, null);
         }
      }

      //--GROUP BY
      if (stmt.groups.size() > 0)
      {
         if (sql.group == null)
            sql.group = "GROUP BY ";

         for (String group : stmt.groups)
         {
            if (!sql.group.endsWith("GROUP BY "))
               sql.group += ", ";
            sql.group += asCol(group);
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
         if (sql.order == null)
            sql.order = "ORDER BY ";

         for (Order order : stmt.order)
         {
            if (!sql.order.endsWith("ORDER BY "))
               sql.order += ", ";

            sql.order += order.col + " " + order.dir;
         }
      }

      //-- now setup the LIMIT clause based
      //-- off of the  "offset" and "limit"
      //-- params OR the "page" and "pageSize"
      //-- query params.  

      if (stmt.pagenum >= 0 && stmt.pagesize >= 0)
      {
         stmt.offset = (stmt.pagenum - 1) * stmt.pagesize;
         stmt.limit = stmt.pagesize;
      }

      if (stmt.limit >= 0 || stmt.offset >= 0)
      {
         sql.limit = "LIMIT ";
         if (stmt.offset >= 0)
            sql.limit += stmt.offset;

         if (stmt.limit >= 0)
         {
            if (!sql.limit.endsWith("LIMIT "))
               sql.limit += ", ";

            sql.limit += stmt.limit;
         }
      }

      //--compose the final statement
      String buff = sql.select;

      buff += " \r\n" + sql.from;

      if (sql.where != null)
         buff += " \r\n" + sql.where;

      if (sql.group != null)
         buff += " \r\n" + sql.group;

      if (sql.order != null)
         buff += " \r\n" + sql.order;

      if (sql.limit != null)
         buff += " \r\n" + sql.limit;

      stmt.sql = buff.toString();

      return stmt;
   }

   public static Stmt buildStmt(Map<String, String> params, Replacer r) throws Exception
   {
      List<Predicate> predicates = new ArrayList();
      Stmt stmt = new Stmt();

      for (String key : (Set<String>) params.keySet())
      {
         if (J.empty(key))
            continue;

         String value = (String) params.get(key);

         //skip if this is a reserved keyword
         if (!key.equalsIgnoreCase("q") && //
               !key.equalsIgnoreCase("filter") && //
               RESERVED_NAMES.contains(key.toLowerCase()))
            continue;

         String clauseStr = null;

         if (key.equalsIgnoreCase("q") || key.equalsIgnoreCase("filter"))
         {
            clauseStr = value;
         }
         else if (J.empty(value) && key.indexOf("(") > -1)
         {
            clauseStr = key;
         }
         else
         {
            clauseStr = key + "=" + value;
         }

         predicates.add(parse(clauseStr));
      }

      for (Predicate p : predicates)
      {
         buildStmt(stmt, p);
      }

      return stmt;
   }

   private static void buildStmt(Stmt stmt, Predicate p) throws Exception
   {
      String token = p.token;

      if (token.endsWith("("))
         token = token.substring(0, token.length() - 1);

      if (WHERE_OPS.contains(token.toLowerCase()))
      {
         stmt.where.add(p);
      }
      else if ("group".equalsIgnoreCase(token))
      {
         for (int i = 0; i < p.terms.size(); i++)
         {
            stmt.addGroupBy(p.term(i).token.replace('\'', '`'));
         }
      }
      else if ("includes".equalsIgnoreCase(token))
      {
         check(p.getParent() == null, "Token 'includes' is not valid as a nested function.");

         List<String> cols = new ArrayList();

         for (int i = 0; i < p.terms.size(); i++)
         {
            cols.add(p.term(i).token);
         }
         stmt.setCols(cols);
      }
      else if ("distinct".equalsIgnoreCase(token))
      {
         check(p.getParent() == null, "Token 'distinct' is not valid as a nested function");

         stmt.distinct = true;
      }
      else if ("function".equalsIgnoreCase(token) || "aggregate".equalsIgnoreCase(token))
      {
         String func = dequote(p.term(0).token);//.replace("`", "").replace("\'", "");
         String col = asCol(p.term(1).token);//.replace("\'", "`");

         Predicate newP = new Predicate(func);
         for (int i = 1; i < p.terms.size(); i++)
         {
            newP.addTerm(p.term(i));
         }

         String as = p.terms.size() > 2 ? asCol(p.term(2).token) : null;

         stmt.addCol(as, newP);
         stmt.addGroupBy(col);
      }
      else if ("sum".equalsIgnoreCase(token) || "count".equalsIgnoreCase(token) || "min".equalsIgnoreCase(token) || "max".equalsIgnoreCase(token))
      {
         check((p.getParent() == null && p.terms.size() <= 2) || p.terms.size() == 1, "Function " + token + " may only have a second 'as' paramter if it is the root function");

         String as = p.terms.size() == 2 ? p.term(1).token : null;

         stmt.addCol(as, p);
      }
      else if ("rowcount".equalsIgnoreCase(token))
      {
         stmt.rowcount = "rowCount";
         if (p.terms.size() > 0)
            stmt.rowcount = dequote(p.term(0).token);
      }
      else if ("countascol".equalsIgnoreCase(token))
      {
         String col = p.term(0).token;

         for (int i = 1; i < p.terms.size(); i++)
         {
            Predicate countif = parse("sum(if(eq(" + col + ", " + p.term(i).token + "), 1, 0))");
            stmt.addCol(check(p.term(i).token), countif);
         }

         String str = "in(" + col;
         for (int i = 1; i < p.terms.size(); i++)
         {
            str += "," + p.term(i).token;
         }
         Predicate in = parse(str);
         stmt.where.add(in);
      }
      else if ("as".equalsIgnoreCase(token))
      {
         stmt.addCol(p.term(1).token, p.term(0));
      }
      else if ("offset".equalsIgnoreCase(token))
      {
         stmt.offset = Integer.parseInt(p.term(0).token);
         if (p.terms.size() > 1)
         {
            stmt.limit = Integer.parseInt(p.term(1).token);
         }
      }
      else if ("limit".equalsIgnoreCase(token))
      {
         stmt.limit = Integer.parseInt(p.term(0).token);
         if (p.terms.size() > 1)
         {
            stmt.offset = Integer.parseInt(p.term(1).token);
         }
      }
      else if ("page".equalsIgnoreCase(token) || "pagenum".equalsIgnoreCase(token))
      {
         stmt.pagenum = Integer.parseInt(p.term(0).token);
         if (p.terms.size() > 1)
         {
            stmt.pagesize = Integer.parseInt(p.term(1).token);
         }
      }
      else if ("pagesize".equalsIgnoreCase(token))
      {
         stmt.pagesize = Integer.parseInt(p.term(0).token);
      }
      else if ("order".equalsIgnoreCase(token) || "sort".equalsIgnoreCase(token))
      {
         for (int i = 0; i < p.terms.size(); i++)
         {
            String sort = p.term(i).token;
            String desc = sort.indexOf('-') > -1 ? "DESC" : "ASC";
            sort = sort.replace("-", "");
            sort = sort.replace("+", "");
            sort = sort.replace('\'', '`');
            stmt.order.add(new Order(sort, desc));
         }
      }
      else if (WHERE_OPS.contains(token.toLowerCase()))
      {
         stmt.where.add(p);
      }
      else
      {
         throw new RuntimeException("Unable to parse: " + p);
      }

      //return sql.toString();
   }

   private static String print(Predicate p, Replacer r, String col) throws Exception
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

      List<String> terms = new ArrayList();
      for (int i = 0; i < p.terms.size(); i++)
      {
         terms.add(print(p.terms.get(i), r, col));
      }

      List<String> preReplace = new ArrayList(terms);

      //allows for callers to substitute callable statement "?"s
      //and/or to account for data type conversion 
      for (int i = 0; col != null && i < p.terms.size(); i++)
      {
         String val = terms.get(i);
         if (isLiteral(val) && r != null)
         {
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
            sql.append(term1).append(" <> ").append(term2);
         }
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
         String s = token.toUpperCase() + "(" + swapIf(acol, '\'', '`') + ")";
         sql.append(s);

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

   /**
    * name=value
    * name=[eq|ne|lt|le|gt|ge]=value
    * name=in=(val,...)
    * name=out=(val,...)
    * [and|or|not]({clause},...)
    * ({clause} [,|and|or|not] {clause})
    * (name=value and name2=in=(1,2,3,4))
    * (eq(name,value) and in(name2, (1,2,3,4))
    * and(eq(name, value), in(name2, (1,2,3,4))
    * 
    * @param parent
    * @param clauseStr
    * @return
    * @throws Exception
    */
   public static Predicate parse(String clause) throws Exception
   {
      //System.out.println("PARSE: " + clause);

      String field = null;
      String op = null;
      String value = null;

      String token = null;
      Tokenizer t = new Tokenizer(clause);

      Predicate root = null;
      List<Predicate> predicates = new ArrayList();

      while ((token = t.next()) != null)
      {
         //System.out.println(token);
         //three supported forms:
         // 1. name first - ex: name=gt=value and (lt=value2 or gt=value3)
         // 2. operator first - ex: gt(name, value) ex: or({clause}, {clause})
         // 3. parens first - ex: (name eq value and (name2 eq value2 or name3 eq value3))

         //System.out.println("TOKEN: \"" + token + "\"");

         String lc = token.toLowerCase();
         String func = lc.endsWith("(") ? lc.substring(0, lc.length() - 1) : null;

         if (FUNCTIONS.contains(lc) && predicates.size() == 1 && "eq".equals(root.token))
         {
            //parses things like "firstname=in=fred,george,john"
            root.token = lc;
         }
         else if (FUNCTIONS.contains(func) || //this line matches tokens like "function(value)"
               (predicates.size() == 0 && FUNCTIONS.contains(lc))) //this line matches tokens like "function=value" which is a shortcut for "function(value)"
         {
            func = func != null ? func : lc;

            Predicate p = new Predicate(func);
            Predicate top = predicates.size() > 0 ? predicates.get(predicates.size() - 1) : null;
            if (top != null)
            {
               top.addTerm(p);
            }
            predicates.add(p);

            if (root == null)
               root = p;

            continue;
         }
         else if (")".equals(lc))
         {
            if (predicates.size() == 0)
            {
               throw new RuntimeException("found an unexpected ')': \"" + clause + "\"");
            }
            predicates.remove(predicates.size() - 1);
         }
         else
         {
            Predicate top = predicates.size() == 0 ? null : predicates.get(predicates.size() - 1);
            if (top == null)
            {
               top = new Predicate();
               predicates.add(top);
               root = top;
            }
            else if ("(".equals(top.token))
            {
               Predicate p = new Predicate();
               top.addTerm(p);
               predicates.add(p);
               top = p;
            }

            if (!t.wasQuoted() && OPERATORS.contains(lc))
            {
               if ("=".equals(lc))
               {
                  if (top.token == null)
                  {
                     top.token = "eq";
                  }
               }
               else
               {
                  top.token = lc;
               }
            }
            else
            {
               if ("and".equals(top.token) || "or".equals(top.token))
               {
                  //hybrid form
                  //ex: or(state=ga, state=eq=sc)
                  Predicate p = new Predicate();
                  p.addTerm(new Predicate(token));
                  top.addTerm(p);
                  predicates.add(p);
               }
               else
               {
                  //                  if (top.terms.size() == 2 && !(top.field.equals("and(") || top.field.equals("or(")))
                  //                  {
                  //                     //this is a forced pop of a a binary operator that
                  //                     //was not ended with a ')' character
                  //                     //ex: or(state=ga, state=sc)
                  //                     //     the 'state=ga' and successively state=sc
                  //                     //     would be popped by this section
                  //                     predicates.remove(predicates.size() - 1);
                  //                     top = predicates.get(predicates.size() - 1);
                  //                     Predicate newP = new Predicate();
                  //                     top.addTerm(newP);
                  //                     predicates.add(newP);
                  //                     top = newP;
                  //                  }

                  top.addTerm(new Predicate(token));
               }
            }
         }
      }

      quote(root);

      //System.out.println("parse: " + clause + " -> " + root);

      root.setSrc(clause);

      return root;
   }

   public static void quote(Predicate p)
   {
      for (int i = 0; i < p.terms.size(); i++)
      {
         if (p.term(i).terms.size() != 0)
            continue;

         if (i == 0)
         {
            String t = p.term(i).token;
            if (isLiteral(t))
            {
               continue;
            }
            else if (!t.startsWith("`"))
            {
               p.term(i).token = "`" + p.term(i).token + "`";
            }
         }
         else
         {
            p.term(i).token = asLiteral(p.term(i).token);
         }
      }

      for (int i = 0; i < p.terms.size(); i++)
      {
         quote(p.term(i));
      }
   }

   public static class Replacer
   {
      HashSet      ignored = new HashSet(Arrays.asList(new String[]{"as", "includes", "sort", "order", "offset", "limit", "distinct", "aggregate", "function", "sum", "count", "min", "max"}));
      List<String> cols    = new ArrayList();
      List<String> vals    = new ArrayList();

      public String replace(Predicate p, String col, String val) throws Exception
      {
         if (val == null || val.trim().equalsIgnoreCase("null"))
            return "NULL";

         if (ignored.contains(p.token.toLowerCase()))
            return val;

         if (val.indexOf('*') > 0)
            val = val.replace('*', '%');

         if (col.startsWith("`"))
         {
            System.out.println("REPLACER:" + col + " = " + val);
            cols.add(col);
            vals.add(val);
            return "?";
         }

         return val;
      }
   }

   public static class Stmt
   {

      String                     sql          = null;

      Parts                      input        = null;

      public boolean             restrictCols = false;

      //url arg order is undependable so order does not matter here either
      HashMap<String, Predicate> cols         = new LinkedHashMap();

      boolean                    distinct     = false;

      public List<Predicate>     where        = new ArrayList();
      public List<String>        groups       = new ArrayList();
      public List<Order>         order        = new ArrayList();

      public int                 pagenum      = -1;
      public int                 pagesize     = -1;

      public int                 limit        = -1;
      public int                 offset       = -1;

      public String              rowcount     = null;

      void addGroupBy(String col)
      {
         col = dequote(col);
         if (!groups.contains(col))
            groups.add(col);
      }

      void addCol(String name, Predicate predicate)
      {
         if (name == null)
            name = "$$$ANON" + UUID.randomUUID().toString();
         cols.put(dequote(name), predicate);
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
            newCols.put(dequote(c), null);
         }

         for (String c : cols.keySet())
         {
            newCols.put(c, cols.get(c));
         }

         cols = newCols;
      }
   }

   static class Order
   {
      String col = null;
      String dir = null;

      public Order(String col, String dir)
      {
         this.col = col;
         this.dir = dir;
      }
   }

   static class Function
   {
      String function = null;
      String col      = null;
      String as       = null;

      public Function(String function, String col, String as)
      {
         super();
         this.function = function;
         this.col = col;
         this.as = as;
      }
   }

   public static class Parts
   {
      String select = "";
      String from   = "";
      String where  = "";
      String group  = "";
      String order  = "";
      String limit  = "";

      protected String chop(String haystack, String start, String... ends)
      {
         String lc = haystack.toLowerCase();

         int idx1 = lc.indexOf(start);
         if (idx1 >= 0)
         {
            int end = firstLastIdx(lc, ends);
            if (end < 0)
               end = lc.length();

            return haystack.substring(idx1, end).trim() + " ";
         }
         return null;
      }

      protected String chopFirst(String haystack, String start, String... ends)
      {
         String lc = haystack.toLowerCase();

         int idx1 = lc.indexOf(start);
         if (idx1 >= 0)
         {
            int end = firstIdx(lc, ends);
            if (end < 0)
               end = lc.length();

            return haystack.substring(idx1, end).trim() + " ";
         }
         return null;
      }

      public Parts(String sql)
      {
         select = chopFirst(sql, "select ", " from");

         sql = sql.substring(select.length(), sql.length());

         if (sql.trim().substring(4).trim().startsWith("("))
         {
            int end = sql.lastIndexOf("as ") + 3;
            String rest = sql.substring(end, sql.length());
            int other = firstIdx(rest, "where ", " group by ", "order ", " limit ");
            if (other > -1)
            {
               end += other;
            }
            else
            {
               end = sql.length();
            }

            from = sql.substring(0, end);
            sql = sql.substring(end, sql.length());
         }
         else
         {
            from = chop(sql, "from ", "where ", " group by ", " order ", " limit ");
         }
         where = chop(sql, "where ", " group by ", " order ", " limit ");
         group = chop(sql, "group by ", " order ", " limit ");
         order = chop(sql, "order ", " limit ");
         limit = chop(sql, "limit ");
      }

      /**
       * Finds the 
       * @param haystack
       * @param needles
       * @return
       */
      static int firstLastIdx(String haystack, String... needles)
      {
         int idx = -1;
         for (String needle : needles)
         {
            int i = haystack.lastIndexOf(needle);
            if (idx == -1 || (i >= 0 && i < idx))
               idx = i;
         }
         return idx;
      }

      static int firstIdx(String haystack, String... needles)
      {
         int idx = -1;
         for (String needle : needles)
         {
            int i = haystack.indexOf(needle);
            if (idx == -1 || (i >= 0 && i < idx))
               idx = i;
         }
         return idx;
      }
   }

   public static class Tokenizer
   {
      char[]       chars  = null;
      int          head   = 0;
      boolean      quoted = false;

      StringBuffer next   = new StringBuffer();

      public Tokenizer(String chars)
      {
         this.chars = chars.toCharArray();
      }

      public boolean wasQuoted()
      {
         return quoted;
      }

      public String next()
      {
         quoted = false;
         
         if (head >= chars.length)
            return null;
         boolean escape = false;
         boolean doubleQuote = false;
         boolean singleQuote = false;
         boolean done = false;

         for (; head < chars.length && !done; head++)
         {
            char c = chars[head];
            switch (c)
            {
               case '(':
                  next.append(c);
                  if (escape || doubleQuote || singleQuote)
                  {
                     continue;
                  }
                  done = true;
                  break;
               case ',':
               case ' ':
                  if (escape || doubleQuote || singleQuote)
                  {
                     next.append(c);
                     continue;
                  }
                  if (next.length() == 0)
                  {
                     continue;
                  }
                  else
                  {
                     done = true;
                     break;
                  }
               case '=':
               case ')':
                  if (escape)
                  {
                     next.append(c);
                     continue;
                  }

                  if (next.length() == 0)
                  {
                     next.append(c);
                  }
                  else
                  {
                     head--;
                  }
                  done = true;
                  break;

               case '\"':
                  if (escape || singleQuote)
                  {
                     next.append(c);
                     continue;
                  }
                  quoted = true;
                  doubleQuote = !doubleQuote;

                  if (!doubleQuote)
                  {
                     done = true;
                     break;
                  }
                  continue;

               case '\'':
                  if (escape || doubleQuote)
                  {
                     next.append(c);
                     continue;
                  }
                  quoted = true;
                  singleQuote = !singleQuote;

                  if (!singleQuote)
                  {
                     done = true;
                     break;
                  }
                  continue;

               case '\\':
                  if (escape)
                  {
                     next.append('\\');
                  }
                  escape = !escape;
                  continue;

               default :
                  next.append(c);
            }
         }

         if (doubleQuote || escape)
            throw new RuntimeException("Unable to parse string: \"" + chars + "\"");

         String str = next.toString();
         next = new StringBuffer();
         return str;
      }
   }

   public static class Predicate
   {
      Predicate       parent = null;
      String          src    = null;

      String          token  = null;
      List<Predicate> terms  = new ArrayList();

      Predicate()
      {

      }

      Predicate(String field)
      {
         this.token = field;
      }

      public String getSrc()
      {
         return src;
      }

      public void setSrc(String src)
      {
         this.src = src;
      }

      void addTerm(Predicate p)
      {
         terms.add(p);
         p.setParent(this);
      }

      public Predicate term(int index)
      {
         return terms.get(index);
      }

      public Predicate getParent()
      {
         return parent;
      }

      public void setParent(Predicate parent)
      {
         this.parent = parent;
      }

      public String toString()
      {
         String token = this.token + "";
         StringBuffer buff = new StringBuffer(token);
         if (terms.size() > 0)
         {
            buff.append("(");

            for (int i = 0; i < terms.size(); i++)
            {
               buff.append(terms.get(i).toString());
               if (i < terms.size() - 1)
                  buff.append(",");
            }

            buff.append(")");
         }

         return buff.toString();
      }
   }

   public static String asLiteral(String val)
   {
      if (val.startsWith("`"))
         return val;

      if (val.startsWith("'"))
         return val;

      try
      {
         Float.parseFloat(val);
         return val;
      }
      catch (Exception ex)
      {
         //not a number, ignore
      }

      if ("true".equalsIgnoreCase(val))
         return "1";

      if ("false".equalsIgnoreCase(val))
         return "0";

      if ("null".equalsIgnoreCase(val))
         return "NULL";

      return "'" + val + "'";

   }

   static boolean isLiteral(String str)
   {
      if (str.startsWith("'"))
         return true;

      if (str.startsWith("`"))
         return false;

      try
      {
         Float.parseFloat(str);
         return true;
      }
      catch (Exception ex)
      {
         //not a number, ignore
      }

      if ("true".equalsIgnoreCase(str))
         return true;

      if ("false".equalsIgnoreCase(str))
         return true;

      if ("null".equalsIgnoreCase(str))
         return true;

      return false;
   }

   public static String quote(String str, char c)
   {
      if (str == null)
         return str;

      if (str.length() == 0)
         return c + "" + c;

      if (str.charAt(0) != c)
         str = c + str;

      if (str.length() == 1 || str.charAt(str.length() - 1) != c)
         str += c;

      return str;
   }

   public static String dequote(String str)
   {
      if (str == null)
         return null;

      while (str.startsWith("`"))
         str = str.substring(1, str.length());

      while (str.startsWith("'"))
         str = str.substring(1, str.length());

      while (str.endsWith("`"))
         str = str.substring(0, str.length() - 1);

      while (str.endsWith("'"))
         str = str.substring(0, str.length() - 1);

      return str;
   }

   static String swapIf(String str, char target, char replacement)
   {
      if (str == null || str.length() < 2)
         return str;

      if (str.charAt(0) == target)
         str = replacement + str.substring(1, str.length());

      if (str.charAt(str.length() - 1) == target)
         str = str.substring(0, str.length() - 1) + replacement;

      return str;
   }

   public static String asStr(String str)
   {
      return quote(str, '\'');
   }

   public static String asCol(String str)
   {
      return quote(str, '`');
   }

   static void check(boolean condition, Object error)
   {
      if (!condition)
         throw new ApiException(SC.SC_400_BAD_REQUEST, "Unable to parse q/filter terms. Reason: " + error);
   }

   public static String check(String sql) throws Exception
   {
      for (int i = 0; i < ILLEGALS.length; i++)
      {
         Matcher m = ILLEGALS[i].matcher(sql);
         if (m.find())
            throw new SQLException("Sql injection attack blocker on keyword \"" + ILLEGALS_REGX[i].trim() + "\".  You have modifying sql in a select statement: " + sql);
      }
      return sql;
   }

}
