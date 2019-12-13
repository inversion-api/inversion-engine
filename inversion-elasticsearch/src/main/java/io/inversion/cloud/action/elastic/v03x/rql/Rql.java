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
package io.inversion.cloud.action.elastic.v03x.rql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.model.Table;
import io.inversion.cloud.utils.SqlUtils;
import io.inversion.cloud.utils.Utils;

public class Rql
{

   //public static final HashSet<String> RESERVED  = new HashSet(Arrays.asList(new String[]{"as", "includes", "sort", "order", "offset", "limit", "distinct", "aggregate", "function", "sum", "count", "min", "max"}));

   /**
    * Words that are reserved as part of the target query dialect
    */
   public static final HashSet<String> RESERVED     = new HashSet();

   /**
    * Words that have other meaning elsewhere and should not be considered
    */
   public static final HashSet<String> EXCLUDED     = new HashSet<String>(                                                                                                                                                                     //
                                                                          Arrays.asList(                                                                                                                                                       //
                                                                                new String[]{"q", "filter", "expands", "excludes", "format", "replace", "ignores"}                                                                             //
                                                                          ));
   /**
    * Words that translate into a filter/where condition
    */
   public static final HashSet<String> CONDITIONALS = new HashSet<String>(Arrays.asList(new String[]{"n", "nn", "nemp", "emp", "w", "wo", "ew", "sw", "eq", "ne", "lt", "le", "gt", "ge", "in", "out", "if", "or", "and", "miles", "search"}));

   static HashSet<String>              FUNCTIONS    = new HashSet<String>(                                                                                                                                                                     //
                                                                          Arrays.asList(                                                                                                                                                       //
                                                                                new String[]{                                                                                                                                                  //
                                                                                      "(",                                                                                                                                                     //
                                                                                      "eq", "ne", "lt", "le", "gt", "ge",                                                                                                                      //
                                                                                      "and", "or",                                                                                                                                             //
                                                                                      "in", "out",                                                                                                                                             //
                                                                                      "group", "count", "sum", "min", "max", "aggregate", "function", "countascol", "rowcount",                                                                //
                                                                                      "miles", "if", "as",                                                                                                                                     //
                                                                                      "includes", "sort", "order", "offset", "limit",                                                                                                          //
                                                                                      "page", "pagenum", "pagesize", "distinct",                                                                                                               // 
                                                                                      "sw", "ew", "w", "wo",                                                                                                                                   //
                                                                                      "emp", "nemp", "nn", "n",                                                                                                                                // nn == not null, n == null
                                                                                      "search"}                                                                                                                                                //
                                                                          ));

   /**
    * Tokens that can be used in the for col=value or col=ge=value.  
    */
   static HashSet<String>              OPERATORS    = new HashSet<String>(                                                                                                                                                                     //
                                                                          Arrays.asList(                                                                                                                                                       //
                                                                                new String[]{"=", "eq", "ne", "lt", "le", "gt", "ge", "in", "out"}));

   static HashMap<String, Rql>         RQLS         = new HashMap();

   public static void addRql(Rql rql)
   {
      RQLS.put(rql.getType().toLowerCase(), rql);
   }

   public static Rql getRql(String type)
   {
      return RQLS.get(type.toLowerCase());
   }

   /**
    * Type should match Db.getType()
    */
   String          type            = null;
   Set<String>     operators       = new HashSet();
   Set<String>     functions       = new HashSet();
   Set<String>     reserved        = new HashSet();
   Set<String>     excluded        = new HashSet();
   Set<String>     conditionals    = new HashSet();

   public Parser   parser          = null;

   private boolean doQuote         = true;

   char            stringQuote     = '\'';
   char            identifierQuote = '"';

   boolean         calcRowsFound   = false;

   protected Rql(String type)
   {
      this.type = type;
      this.parser = new Parser(this);

      setReserved(RESERVED);
      setOperators(OPERATORS);
      setFunctions(FUNCTIONS);
      setExcluded(EXCLUDED);
      setConditionals(CONDITIONALS);

   }

   public boolean isFunction(String str)
   {
      if (str == null)
         return false;
      return functions.contains(str.toLowerCase());
   }

   public boolean isOperator(String str)
   {
      if (str == null)
         return false;
      return operators.contains(str.toLowerCase());
   }

   public boolean isReserved(String str)
   {
      if (str == null)
         return false;
      return reserved.contains(str.toLowerCase());
   }

   public boolean isExcluded(String str)
   {
      if (str == null)
         return false;
      return excluded.contains(str.toLowerCase());
   }

   public boolean isConditional(String str)
   {
      if (str == null)
         return false;
      return conditionals.contains(str.toLowerCase());
   }

   public Parser getParser()
   {
      return parser;
   }

   public String getType()
   {
      return type;
   }

   public void setType(String type)
   {
      this.type = type;
   }

   public char getStringQuote()
   {
      return stringQuote;
   }

   public void setStringQuote(char stringQuote)
   {
      this.stringQuote = stringQuote;
   }

   public char getIdentifierQuote()
   {
      return identifierQuote;
   }

   public void setIdentifierQuote(char identifierQuote)
   {
      this.identifierQuote = identifierQuote;
   }

   public boolean isDoQuote()
   {
      return doQuote;
   }

   public void setDoQuote(boolean doQuote)
   {
      this.doQuote = doQuote;
   }

   public boolean isCalcRowsFound()
   {
      return calcRowsFound;
   }

   public void setCalcRowsFound(boolean calcRowsFound)
   {
      this.calcRowsFound = calcRowsFound;
   }

   public void addOperator(String operator)
   {
      operators.add(operator.toLowerCase());
   }

   public void addReserved(String reserved)
   {
      this.reserved.add(reserved.toLowerCase());
   }

   public void addFunction(String function)
   {
      this.functions.add(function.toLowerCase());
   }

   public void addExcluded(String excluded)
   {
      this.excluded.add(excluded.toLowerCase());
   }

   public void addConditional(String conditional)
   {
      this.conditionals.add(conditional.toLowerCase());
   }

   public void setOperators(Collection<String> operators)
   {
      this.operators.clear();
      for (String word : operators)
         addOperator(word);
   }

   public void setReserved(Collection<String> reserved)
   {
      this.reserved.clear();
      for (String word : reserved)
         addReserved(word);
   }

   public void setFunctions(Collection<String> functions)
   {
      this.functions.clear();
      for (String word : functions)
         addFunction(word);
   }

   public void setExcluded(Collection<String> excluded)
   {
      this.excluded.clear();
      for (String word : excluded)
         addExcluded(word);
   }

   public void setConditionals(Collection<String> conditionals)
   {
      this.conditionals.clear();
      for (String word : conditionals)
         addConditional(word);
   }

   public Stmt createStmt(String sqlStr, Table table, Map<String, String> params) throws Exception
   {
      return createStmt(sqlStr, table, params, null);
   }

   public Stmt createStmt(String sqlStr, Table table, Map<String, String> params, Replacer r) throws Exception
   {
      String ignoresStr = params.get("ignores");

      if (!Utils.empty(ignoresStr))
      {
         List ignores = Utils.explode(",", ignoresStr.toLowerCase());
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
            String value = SqlUtils.check(params.get(key));
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
      Stmt stmt = new Stmt(this, sql, r, table);
      buildStmt(stmt, table, params, r);

      return stmt;
   }

   protected Stmt buildStmt(Stmt stmt, Table table, Map<String, String> params, Replacer r) throws Exception
   {
      List<Predicate> predicates = new ArrayList<Predicate>();

      for (String key : (Set<String>) params.keySet())
      {
         if (Utils.empty(key))
            continue;

         String value = (String) params.get(key);

         //skip if this is a reserved keyword
         if (!key.equalsIgnoreCase("q") && //
               !key.equalsIgnoreCase("filter") && //
               isExcluded(key.toLowerCase()))
            continue;

         String clauseStr = null;

         if (key.equalsIgnoreCase("q") || key.equalsIgnoreCase("filter"))
         {
            clauseStr = value;
         }
         else if (Utils.empty(value) && key.indexOf("(") > -1)
         {
            clauseStr = key;
         }
         else
         {
            clauseStr = key + "=" + value;
         }

         Predicate pred = parser.parse(clauseStr);
         predicates.add(pred);
      }

      for (Predicate p : predicates)
      {
         buildStmt(table, stmt, p);
      }

      return stmt;
   }

   protected void buildStmt(Table table, Stmt stmt, Predicate p) throws Exception
   {
      String token = p.token;

      if (token.endsWith("("))
         token = token.substring(0, token.length() - 1);

      if (isConditional(token.toLowerCase()))
      {
         stmt.where.add(p);
      }
      else if ("group".equalsIgnoreCase(token))
      {
         for (int i = 0; i < p.terms.size(); i++)
         {
            stmt.addGroupBy(p.term(i).token);
         }
      }
      else if ("includes".equalsIgnoreCase(token))
      {
         check(p.getParent() == null, "Token 'includes' is not valid as a nested function.");

         List<String> cols = new ArrayList<String>();

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
         String func = Parser.dequote(p.term(0).token);
         String col = parser.asCol(p.term(1).token);

         Predicate newP = new Predicate(func);
         for (int i = 1; i < p.terms.size(); i++)
         {
            newP.addTerm(p.term(i));
         }

         String as = p.terms.size() > 2 ? parser.asCol(p.term(2).token) : null;

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
            stmt.rowcount = p.term(0).token;
      }
      else if ("countascol".equalsIgnoreCase(token))
      {
         String col = p.term(0).token;

         for (int i = 1; i < p.terms.size(); i++)
         {
            Predicate countif = parser.parse("sum(if(eq(" + col + ", " + p.term(i).token + "), 1, 0))");
            stmt.addCol(SqlUtils.check(p.term(i).token), countif);
         }

         String str = "in(" + col;
         for (int i = 1; i < p.terms.size(); i++)
         {
            str += "," + p.term(i).token;
         }
         Predicate in = parser.parse(str);
         stmt.where.add(in);
      }
      else if ("as".equalsIgnoreCase(token))
      {
         stmt.addCol(p.term(1).token, p.term(0));
      }
      else if ("offset".equalsIgnoreCase(token))
      {
         stmt.offset = toInt("offest", p.term(0).token);
         if (p.terms.size() > 1)
         {
            stmt.limit = toInt("limit", p.term(1).token);
         }
      }
      else if ("limit".equalsIgnoreCase(token))
      {
         stmt.limit = toInt("limit", p.term(0).token);
         if (p.terms.size() > 1)
         {
            stmt.offset = toInt("offset", p.term(1).token);
         }
      }
      else if ("page".equalsIgnoreCase(token) || "pagenum".equalsIgnoreCase(token))
      {
         stmt.pagenum = toInt(token, p.term(0).token);
         if (p.terms.size() > 1)
         {
            stmt.pagesize = toInt("pagesize", p.term(1).token);
         }
      }
      else if ("pagesize".equalsIgnoreCase(token))
      {
         stmt.pagesize = toInt(token, p.term(0).token);
      }
      else if ("order".equalsIgnoreCase(token) || "sort".equalsIgnoreCase(token))
      {
         for (int i = 0; i < p.terms.size(); i++)
         {
            String sort = parser.asCol(p.term(i).token);
            String desc = sort.indexOf('-') > -1 ? "DESC" : "ASC";
            sort = sort.replace("-", "");
            sort = sort.replace("+", "");
            stmt.order.add(new Order(sort, desc));
         }
      }
      else if (isConditional(token.toLowerCase()))
      {
         stmt.where.add(p);
      }
      else
      {
         throw new RuntimeException("Unable to parse: " + p);
      }
   }

   void check(boolean condition, Object error)
   {
      if (!condition)
         throw new ApiException(SC.SC_400_BAD_REQUEST, "Unable to parse q/filter terms. Reason: " + error);
   }

   public int toInt(String field, String inStr)
   {
      String str = inStr;
      try
      {
         while (str.startsWith("`"))
            str = str.substring(1, str.length());

         while (str.endsWith("`"))
            str = str.substring(0, str.length() - 1);

         while (str.startsWith("'"))
            str = str.substring(1, str.length());

         while (str.endsWith("'"))
            str = str.substring(0, str.length() - 1);

         while (str.startsWith("\""))
            str = str.substring(1, str.length());

         while (str.endsWith("\""))
            str = str.substring(0, str.length() - 1);

         String limit = str.trim();
         return Integer.parseInt(limit);
      }
      catch (Exception ex)
      {
         throw new ApiException(SC.SC_400_BAD_REQUEST, "Expected an integer for field \"" + field + "\" but found value \"" + inStr + "\"");
      }
   }

   public String asCol(String string)
   {
      return getParser().asCol(SqlUtils.check(string));
   }

   public String asStr(String string)
   {
      return getParser().asStr(SqlUtils.check(string));
   }

}
