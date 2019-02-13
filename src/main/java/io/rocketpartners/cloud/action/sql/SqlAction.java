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
package io.rocketpartners.cloud.action.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Db;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Request;
import io.rocketpartners.cloud.utils.Sql;
import io.rocketpartners.cloud.utils.Utils;

public abstract class SqlAction<T extends SqlAction> extends Action<T>
{

//   public LinkedHashSet<String> splitParam(Request req, String key)
//   {
//      LinkedHashSet map = new LinkedHashSet();
//      String param = req.getParam(key);
//      if (!J.empty(param))
//      {
//         String[] arr = param.split(",");
//         for (String e : arr)
//         {
//            e = e.trim().toLowerCase();
//            if (!J.empty(e))
//               map.add(e);
//         }
//      }
//
//      return map;
//   }

   public static String nextPath(String path, String next)
   {
      return Utils.empty(path) ? next : path + "." + next;
   }

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
   public static String parseSql(String sql, Chain chain, Action action, Request req, Db db, String table, Collection keys)
   {
      if (sql != null)
      {
         StringBuffer buff = new StringBuffer("");
         Pattern p = Pattern.compile("\\$\\{([^\\}]*)\\}");
         Matcher m = p.matcher(sql);
         while (m.find())
         {
            String value = null;
            String key = m.group(1).toLowerCase();
            if ("table".equals(key))
            {
               value = table;
            }
            else if ("keys".equals(key))
            {
               StringBuffer idstr = new StringBuffer();
               List keyList = keys instanceof List ? (List) keys : new ArrayList(keys);
               for (int i = 0; i < keys.size(); i++)
               {
                  idstr.append(keyList.get(i));
                  if (i < keyList.size() - 1)
                     idstr.append(",");
               }
               value = idstr.toString();
            }
            else if ("user.id".equals(key) || "userid".equals(key))
            {
               value = req.getUser().getId() + "";
            }
            else if ("user.username".equals(key) || "username".equals(key))
            {
               value = req.getUser().getUsername();
            }
            //            else if ("user.perms".equals(key) || "user.permissions".equals(key) || "permissions".equals(key) || "perms".equals(key))
            //            {
            //               value = "";
            //               List<String> perms = new ArrayList(req.getUser().getPerms());
            //               for (int i = 0; i < perms.size(); i++)
            //               {
            //                  String perm = perms.get(i);
            //                  value += db.getQuote() + perm + db.getQuote();
            //                  if (i < perms.size() - 1)
            //                     value += ",";
            //               }
            //            }
            //            else if ("user.roles".equals(key) || "roles".equals(key))
            //            {
            //               value = "";
            //               List<String> roles = new ArrayList(req.getUser().getRoles());
            //               for (int i = 0; i < roles.size(); i++)
            //               {
            //                  String role = roles.get(i);
            //                  value += db.getQuote() + role + db.getQuote();
            //                  if (i < roles.size() - 1)
            //                     value += ",";
            //               }
            //            }
            else if (chain.get(key) != null)
            {
               value = chain.get(key).toString();
            }
            else
            {
               value = req.getParam(key);
            }

            if (value == null)
               throw new ApiException(SC.SC_400_BAD_REQUEST, "Unknown collection filter param: \"" + key + "\"");

            Sql.check(value);//sql injection check

            m.appendReplacement(buff, value);
         }
         m.appendTail(buff);
         sql = buff.toString();
      }
      return sql;
   }

   LinkedHashSet<String> asSet(String value)
   {
      LinkedHashSet set = new LinkedHashSet();
      if (value != null)
      {
         value = value.toLowerCase();
         for (String v : value.split(","))
         {
            v = v.trim();
            if (v.length() > 0)
               set.add(v);
         }
      }
      return set;
   }

   Object cast(io.rocketpartners.cloud.model.Collection coll, String col, Object val)
   {
      String type = null;

      if (coll != null && coll.getEntity().getTable().getColumn(col) != null)
      {
         try
         {
            type = coll.getEntity().getTable().getColumn(col).getType();
         }
         catch (Exception ex)
         {
            ex.printStackTrace();
         }
      }

      if (type == null && (col.endsWith("date") || col.endsWith("at")))
      {
         type = "date";
      }

      if (type == null && ("true".equalsIgnoreCase(val + "") || "false".equalsIgnoreCase(val + "")))
      {
         type = "boolean";
      }

      if (val instanceof String)
      {
         String str = (String) val;
         if (str.startsWith("'") && str.endsWith("'") && str.length() > 1)
            str = str.substring(1, str.length() - 1);
         val = str;
      }

      if (type != null)
      {
         type = type.toLowerCase();
         val = Sql.cast(val, type);
      }

      return val;
   }

}
