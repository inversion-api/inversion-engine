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
package io.rcktapp.api;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.CaseInsensitiveMap;

import io.forty11.j.J;
import io.forty11.js.JSObject;
import io.rcktapp.api.service.Service;

public class Chain
{
   Service            service  = null;
   Api                api      = null;
   List<Rule>         rules    = null;
   Request            request  = null;
   Response           response = null;

   int                next     = 0;
   boolean            canceled = false;

   CaseInsensitiveMap vars     = new CaseInsensitiveMap();

   public Chain(Service service, Api api, List<Rule> rules, Request req, Response res)
   {
      this.service = service;
      this.api = api;
      this.rules = rules;
      this.request = req;
      this.response = res;
   }

   public void put(String key, Object value)
   {
      vars.put(key, value);
   }

   /**
    * Storage for chain steps to communicate with each other.
    * 
    * @param key
    * @return
    */
   public Object get(String key)
   {
      if (vars.containsKey(key))
         return vars.get(key);

      for (int i = next - 1; i >= 0; i--)
      {
         JSObject config = rules.get(i).getConfig();
         if (config != null && config.containsKey(key))
            return config.get(key);
      }
      return null;
   }

   public Object remove(Object key)
   {
      return vars.remove(key);
   }

   public void go() throws Exception
   {
      while (next())
      {
         ;
      }
   }

   public boolean next() throws Exception
   {
      while (!canceled && next < rules.size())
      {
         Rule rule = rules.get(next);

         if (!rule.isAuthroized(request))
            throw new ApiException(SC.SC_401_UNAUTHORIZED);

         next += 1;

         if (!J.empty(rule.getStatus()))
            response.setStatus(rule.getStatus());

         if (rule.isTerminate())
            canceled = true;

         Handler handler = service.getHandler(api, rule.getHandler());
         //rule.getHandler() being null is not an error
         //but not being able to find a named handler is an error
         if (rule.getHandler() != null && handler != null)
         {
            runHandler(rule, handler, request, response);
         }
         else
         {
            throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Unable to find handler '" + rule.getHandler() + "'");
         }

         return !canceled;
      }
      return false;
   }

   void runHandler(Rule rule, Handler handler, Request req, Response response) throws Exception
   {
      JSObject config = rule.getConfig();

      if (config != null)
      {
//         if (config.containsKey("queryDefaults"))
//         {
//            req.withQuery((String) config.get("queryDefaults"), false);
//         }
//
//         if (config.containsKey("queryAbsolutes"))
//         {
//            req.withQuery((String) config.get("queryAbsolutes"), true);
//         }
//
//         if (config.containsKey("query"))
//         {
//            req.setQuery((String) config.get("query"));
//         }

         if (config.containsKey("entityKey"))
         {
            req.setEntityKey((String) config.get("entityKey"));
         }

         if (config.containsKey("collectionKey"))
         {
            req.setCollectionKey((String) config.get("collectionKey"));
         }

         if (config.containsKey("subCollectionKey"))
         {
            req.setSubCollectionKey((String) config.get("subCollectionKey"));
         }
      }
      handler.service(service, this, rule, request, response);
   }


   public int getNext()
   {
      return next;
   }

   public void setNext(int next)
   {
      this.next = next;
   }

   public boolean isCanceled()
   {
      return canceled;
   }

   public void cancel()
   {
      this.canceled = true;
   }

}
