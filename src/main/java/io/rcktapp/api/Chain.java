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
package io.rcktapp.api;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.map.CaseInsensitiveMap;

import io.forty11.j.J;
import io.rcktapp.api.service.Service;

public class Chain
{
   Service            service  = null;
   Api                api      = null;
   Endpoint           endpoint = null;
   List<Action>       actions  = null;
   Request            request  = null;
   Response           response = null;

   int                next     = 0;
   boolean            canceled = false;

   CaseInsensitiveMap vars     = new CaseInsensitiveMap();

   Chain              parent   = null;

   public Chain(Service service, Api api, Endpoint endpoint, List<Action> actions, Request req, Response res)
   {
      this.service = service;
      this.api = api;
      this.endpoint = endpoint;
      this.actions = actions;
      this.request = req;
      this.response = res;
   }

   public void debug(Object... msgs)
   {
      response.debug(msgs);
   }

   public Chain getParent()
   {
      return parent;
   }

   public void setParent(Chain parent)
   {
      this.parent = parent;
   }

   public void put(String key, Object value)
   {
      vars.put(key, value);
   }

   public boolean isDebug()
   {
      if (parent != null)
         return parent.isDebug();

      return request.isDebug();
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
         Object param = actions.get(i).getConfig(key);
         if (!J.empty(param))
            return param;
      }

      if (parent != null)
         return parent.get(key);

      return null;
   }

   public Object remove(Object key)
   {
      return vars.remove(key);
   }

   public Set<String> getConfigSet(String key)
   {
      LinkedHashSet values = new LinkedHashSet();

      String value = null;
      for (int i = next - 1; i >= 0; i--)
      {
         value = actions.get(i).getConfig(key);
         if (value != null)
         {
            for (String str : value.split(","))
               values.add(str.trim());
         }
      }

      value = endpoint.getConfig(key);
      if (value != null)
      {
         for (String str : value.split(","))
            values.add(str.trim());
      }

      return values;
   }

   public int getConfig(String key, int defaultValue)
   {
      return Integer.parseInt(getConfig(key, defaultValue + ""));
   }
   
   public boolean getConfig(String key, boolean defaultValue)
   {
      return Boolean.parseBoolean(getConfig(key, defaultValue + ""));
   }
   
   public String getConfig(String key, String defaultValue)
   {
      String value = actions.get(next - 1).getConfig(key);
      if (!J.empty(value))
      {
         return value;
      }

      value = endpoint.getConfig(key);
      if (!J.empty(value))
      {
         return value;
      }

      return defaultValue;
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
      if (!isCanceled() && next < actions.size())
      {
         Action action = actions.get(next);
         next += 1;
         action.run(service, api, endpoint, this, request, response);
         return true;
      }
      return false;
   }

   public boolean isCanceled()
   {
      return canceled;
   }

   public void cancel()
   {
      this.canceled = true;
   }

   public Service getService()
   {
      return service;
   }

   public Api getApi()
   {
      return api;
   }

   public Endpoint getEndpoint()
   {
      return endpoint;
   }

   public List<Action> getActions()
   {
      return actions;
   }

   public Request getRequest()
   {
      return request;
   }

   public Response getResponse()
   {
      return response;
   }

}
