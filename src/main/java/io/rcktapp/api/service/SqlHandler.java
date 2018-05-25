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

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.forty11.sql.Rows.Row;
import io.forty11.sql.Sql;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Request;
import io.rcktapp.api.Rule;
import io.rcktapp.api.SC;

public abstract class SqlHandler implements Handler
{

   /**
    * Replaces variables of the form '${key}' with values associated with 
    * known keys.
    * 
    * Known keys include:
    * <ol>
    *   <li>'table'
    *   <li>'ids' the ids to be filters
    *   <li>'user.id' || 'userId'
    *   <li>'user.username' || 'username'
    *   <li>'user.perms' || 'user.permissions' || 'permissions' || 'perms'
    *   <li> if none of the above than look for request params w/ the given key
    * </ol>
    *
    * @return
    */
   public Collection filterIds(Rule rule, Request req, Connection conn, String table, List ids)
   {
      return ids;
      //      try
      //      {
      //         if (ids != null && ids.size() > 0 && rule.getConfig() != null && rule.getConfig().get("filter") != null)
      //         {
      //            String filter = rule.getConfig().getString("filter");
      //            if (filter != null)
      //            {
      //               StringBuffer buff = new StringBuffer("");
      //               Pattern p = Pattern.compile("\\$\\{([^\\}]*)\\}");
      //               Matcher m = p.matcher(filter);
      //               while (m.find())
      //               {
      //                  String value = null;
      //                  String key = m.group(1).toLowerCase();
      //                  if ("table".equals(key))
      //                  {
      //                     value = table;
      //                  }
      //                  else if ("ids".equals(key))
      //                  {
      //                     StringBuffer idstr = new StringBuffer();
      //                     for (int i = 0; i < ids.size(); i++)
      //                     {
      //                        idstr.append(ids.get(i));
      //                        if (i < ids.size() - 1)
      //                           idstr.append(",");
      //                     }
      //                     value = idstr.toString();
      //                  }
      //                  else if ("user.id".equals(key) || "userid".equals(key))
      //                  {
      //                     value = req.getUser().getId() + "";
      //                  }
      //                  else if ("user.username".equals(key) || "username".equals(key))
      //                  {
      //                     value = req.getUser().getUsername();
      //                  }
      //                  else if ("user.perms".equals(key) || "user.permissions".equals(key) || "permissions".equals(key) || "perms".equals(key))
      //                  {
      //                     value = "";
      //                     List<String> perms = new ArrayList(req.getUser().getPerms());
      //                     for (int i = 0; i < perms.size(); i++)
      //                     {
      //                        String perm = perms.get(i);
      //                        value += "\"" + perm + "\"";
      //                        if (i < perms.size() - 1)
      //                           value += ",";
      //                     }
      //                  }
      //                  else
      //                  {
      //                     value = req.getParam(key);
      //                  }
      //
      //                  if (value == null)
      //                     throw new ApiException(SC.SC_400_BAD_REQUEST, "Unknown collection filter param: \"" + key + "\"");
      //
      //                  Sql.check(value);//sql injection check
      //
      //                  m.appendReplacement(buff, value);
      //               }
      //               m.appendTail(buff);
      //               filter = buff.toString();
      //            }
      //
      //            return new HashSet(Sql.selectList(conn, filter));
      //         }
      //
      //         return ids;
      //      }
      //      catch (Exception ex)
      //      {
      //         throw ex instanceof ApiException ? (ApiException) ex : new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, ex.getMessage());
      //      }
   }

   public Object filterId(Rule rule, Request req, Connection conn, String table, Object id)
   {
      return id;
      //      Collection ids = filterIds(rule, req, conn, table, Arrays.asList(new Object[]{id}));
      //      if (ids.size() > 0)
      //         return ids.iterator().next();
      //
      //      return null;
   }

   List<Row> filterRows(Rule rule, Request req, Connection conn, String table, String keyCol, List<Row> rows)
   {

      return rows;

      //      if (rows.size() == 0 || rule.getConfig() == null || rule.getConfig().get("filter") == null)
      //         return rows;
      //
      //      ArrayList filtered = new ArrayList();
      //
      //      List ids = new ArrayList();
      //      for (Row row : rows)
      //      {
      //         ids.add(row.get(keyCol));
      //      }
      //
      //      Collection keptIds = filterIds(rule, req, conn, table, ids);
      //
      //      for (Row row : rows)
      //      {
      //         if (keptIds.contains(row.get(keyCol)))
      //            filtered.add(row);
      //      }
      //      return filtered;

   }

}
