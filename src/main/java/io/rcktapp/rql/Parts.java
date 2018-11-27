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
package io.rcktapp.rql;

import io.forty11.j.J;

public class Parts
{
   public String select = "";
   public String from   = "";
   public String where  = "";
   public String group  = "";
   public String order  = "";
   public String limit  = "";

   public Parts(String sql)
   {
      select = chopFirst(sql, "select ", " from");

      if (J.empty(select))
         select = chopFirst(sql, "update ", " where");

      if (J.empty(select))
         select = chopFirst(sql, "delete ", " from");

      if (select != null)
      {
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
}