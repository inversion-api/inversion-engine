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
import java.util.List;

import io.forty11.j.J;

public class Parser
{

   Rql rql;

   //   char                   stringQuote     = '\'';
   //   char                   identifierQuote = '"';
   //
   //   private boolean        doQuote         = true;

   //   private boolean        calcRowsFound   = true;

   public Parser(Rql rql)
   {
      this.rql = rql;

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
   public Predicate parse(String clause) throws Exception
   {
      //System.out.println("PARSE: " + clause);

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

         if (rql.isFunction(lc) && predicates.size() == 1 && "eq".equals(root.token))
         {
            //parses things like "firstname=in=fred,george,john"
            root.token = lc;
         }
         else if (rql.isFunction(func) || //this line matches tokens like "function(value)"
               (predicates.size() == 0 && rql.isFunction(lc))) //this line matches tokens like "function=value" which is a shortcut for "function(value)"
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

            if (rql.isOperator(lc))
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

      if (rql.isDoQuote())
         quote(root);

      root.setSrc(clause);

      return root;
   }

   public void quote(Predicate p)
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
            else if (!t.startsWith(rql.getIdentifierQuote() + ""))
            {
               //p.term(i).token = identifierQuote + p.term(i).token + identifierQuote;

               p.term(i).token = asCol(p.term(i).token);
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

   /**
    * Wraps str in c but will not 
    * double wrap
    * 
    * @param str
    * @param c
    * @return
    */
   String quote(String str, char c)
   {
      if (str == null)
         return str;

      str = dequote(str);

      if (str.length() == 0)
         return c + "" + c;

      if (str.charAt(0) != c)
         str = c + str;

      if (str.length() == 1 || str.charAt(str.length() - 1) != c)
         str += c;

      return str;
   }

   /**
    * Removes all leading and trailing ' and ` characters.
    * 
    * @param str
    * @return
    */
   public static String dequote(String str)
   {
      return dequote(str, '\'', '"', '`');
      //      
      //      if (str == null)
      //         return null;
      //
      //      while (str.length() >= 2 && str.charAt(0) == str.charAt(str.length() - 1) && (str.charAt(0) == '\'' || str.charAt(0) == '"' || str.charAt(0) == '`'))
      //      {
      //         str = str.substring(1, str.length() - 1);
      //      }
      //
      //      return str;
   }

   public static String dequote(String str, char... quoteChars)
   {
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

   /**
    * Swaps the first and last character of str
    * with replacement only if that character is target
    * 
    * @param str
    * @param target
    * @param replacement
    * @return
    */
   public String swapIf(String str, char target, char replacement)
   {
      if (str == null || str.length() < 2)
         return str;

      if (str.charAt(0) == target)
         str = replacement + str.substring(1, str.length());

      if (str.charAt(str.length() - 1) == target)
         str = str.substring(0, str.length() - 1) + replacement;

      return str;
   }

   /**
    * Returns str wrapped in stringQuote
    * 
    * @param str
    * @return
    */
   public String asStr(String str)
   {
      return quote(str, rql.getStringQuote());
   }

   /**
    * Returns str wrapped in identifierQuote
    * @param str
    * @return
    */
   public String asCol(String str)
   {
      int firstDot = str.indexOf(".");
      if (firstDot > 0)
      {
         // if col has a dot, return alias.`colname`
         str = dequote(str);
         String a = str.substring(0, firstDot + 1);
         String b = str.substring(firstDot + 1);
         if (!J.empty(a) && !J.empty(b))
         {
            return a + quote(b, rql.getIdentifierQuote());
         }
      }
      return (quote(dequote(str), rql.getIdentifierQuote()));
   }

   public static String asLiteral(String val)
   {
      if (val.startsWith("`"))
         return val;

      if (val.startsWith("'"))
         return val;

      val = dequote(val);

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

   public boolean isLiteral(String str)
   {
      if (str.startsWith("'"))
         return true;

      if (str.startsWith("`"))
         return false;

      //TODO: need to document why this is differen than others
      if (str.startsWith("\""))
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

   //   public boolean isCalcRowsFound()
   //   {
   //      return calcRowsFound;
   //   }
   //
   //   public void setCalcRowsFound(boolean calcRowsFound)
   //   {
   //      this.calcRowsFound = calcRowsFound;
   //   }

}