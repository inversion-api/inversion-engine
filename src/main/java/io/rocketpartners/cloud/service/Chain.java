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
package io.rocketpartners.cloud.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.collections4.map.CaseInsensitiveMap;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.utils.Utils;

public class Chain
{

   static ThreadLocal<Stack<Chain>> chainLocal = new ThreadLocal();

   protected static Stack<Chain> get()
   {
      Stack stack = chainLocal.get();
      if (stack == null)
      {
         stack = new Stack();
         chainLocal.set(stack);
      }
      return stack;
   }

   public static int getDepth()
   {
      return get().size();
   }

   public static Chain peek()
   {
      Stack<Chain> stack = get();
      if (!stack.empty())
         return stack.peek();
      return null;

   }

   public static Chain push(Service service, Request req, Response res)
   {
      Chain child = new Chain(service, req, res);

      Chain parent = peek();
      if (parent != null)
         child.setParent(parent);

      get().push(child);

      return child;
   }

   public static Chain pop()
   {
      return get().pop();
   }

   public static int size()
   {
      return get().size();
   }

   public static Request getRequest()
   {
      return peek().request;
   }

   public static Response getResponse()
   {
      return peek().response;
   }

   public static void debug(Object... msgs)
   {
      Stack<Chain> stack = get();
      if (stack.size() < 1)
      {
         return;
      }

      String prefix = "[" + stack.size() + "]: ";
      for (int i = 1; i < stack.size(); i++)
         prefix += "   ";

      if (msgs != null && msgs.length == 1 && msgs[0].toString().trim().length() == 0)
         return;

      Chain root = stack.get(0);
      root.response.debug(prefix, msgs);
   }

   public static String buildLink(Collection collection)
   {
      return buildLink(collection, null, null);
   }

   public static String buildLink(Collection collection, Object entityKey, String subCollectionKey)
   {
      String collectionKey = collection.getName();

      Request req = getRequest();

      String url = req.getApiPath();

      if (url == null)
         url = "";

      if (!url.endsWith("/"))
         url += "/";

      if (collection == req.getCollection())
      {
         //going after the same collection...so must be going after the same endpoint
         //so get the endpoint path from the current request and ame sure it is on the url.

         String ep = req.getEndpointPath();

         url += ep;

         if (!url.endsWith("/"))
            url += "/";
      }
      else if (collection != null && collection.getIncludePaths().size() > 0)
      {
         //TODO: need test case here

         String collectionPath = (String) collection.getIncludePaths().get(0);
         if (collectionPath.indexOf("*") > -1)
            collectionPath = collectionPath.substring(0, collectionPath.indexOf("*"));

         url += collectionPath;
         if (!url.endsWith("/"))
            url += "/";
      }

      if (!Utils.empty(collectionKey))
      {
         if (!url.endsWith("/"))
            url += "/";

         url += collectionKey;
      }

      if (!Utils.empty(entityKey))
         url += "/" + entityKey;

      if (!Utils.empty(subCollectionKey))
         url += "/" + subCollectionKey;

      if (req.getApi().getUrl() != null && !url.startsWith(req.getApi().getUrl()))
      {
         String newUrl = req.getApi().getUrl();
         while (newUrl.endsWith("/"))
            newUrl = newUrl.substring(0, newUrl.length() - 1);

         url = newUrl + url.substring(url.indexOf("/", 8));
      }

      if (Utils.empty(collection.getApi().getUrl()))
      {
         String proto = req.getHeader("x-forwarded-proto");
         if (!Utils.empty(proto))
         {
            url = proto + url.substring(url.indexOf(':'), url.length());
         }
      }
      return url;
   }

   protected Service            service  = null;
   protected List<Action>       actions  = new ArrayList();
   protected Request            request  = null;
   protected Response           response = null;

   protected int                next     = 0;
   protected boolean            canceled = false;

   protected CaseInsensitiveMap vars     = new CaseInsensitiveMap();

   protected Chain              parent   = null;

   private Chain(Service service, Request req, Response res)
   {
      this.service = service;
      this.request = req;
      this.response = res;
   }

   //   public void debug(Object... msgs)
   //   {
   //      response.debug(msgs);
   //   }

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

      Object value = getConfig(key);
      if (value != null)
         return value;

      //      for (int i = next - 1; i >= 0; i--)
      //      {
      //         Object param = actions.get(i).getConfig(key);
      //         if (!Utils.empty(param))
      //            return param;
      //      }

      if (parent != null)
         return parent.get(key);

      return null;
   }

   public Object remove(Object key)
   {
      if (vars.containsKey(key))
         return vars.remove(key);

      return getConfig(key + "");
   }

   /**
    * Returns the combined list of endpoint/action stack/request params
    * for the suppled key
    * 
    * This for example, allows you to add to but not remove from
    * a configured "excludes" parameter
    * 
    * All returned values are lower case
    * 
    * @param key
    * @return
    */
   public Set<String> mergeEndpointActionParamsConfig(String key)
   {
      LinkedHashSet values = new LinkedHashSet();

      String value = request.getEndpoint().getConfig(key);
      if (value != null)
      {
         value = value.toLowerCase();
         values.addAll(Utils.explode(",", value));
      }

      for (int i = next - 1; i >= 0; i--)
      {
         value = actions.get(i).getConfig(key);
         if (value != null)
         {
            value = value.toLowerCase();
            values.addAll(Utils.explode(",", value));
         }
      }

      value = request.getParam(key);
      if (value != null)
      {
         value = value.toLowerCase();
         values.addAll(Utils.explode(",", value));
      }

      return values;
   }

   public Map<String, String> getConfig()
   {
      Map<String, String> config = new HashMap();
      for (String key : getConfigKeys())
      {
         config.put(key, getConfig(key));
      }
      return config;
   }

   public Set<String> getConfigKeys()
   {
      Set<String> keys = request.getEndpoint().getConfigKeys();
      for (int i = next - 1; i >= 0; i--)
      {
         keys.addAll(actions.get(i).getConfigKeys());
      }

      return keys;
   }

   public int getConfig(String key, int defaultValue)
   {
      return Integer.parseInt(getConfig(key, defaultValue + ""));
   }

   public boolean getConfig(String key, boolean defaultValue)
   {
      return Boolean.parseBoolean(getConfig(key, defaultValue + ""));
   }

   public String getConfig(String key)
   {
      return getConfig(key, (String) null);
   }

   public String getConfig(String key, String defaultValue)
   {
      String value = request.getEndpoint().getConfig(key);
      if (!Utils.empty(value))
      {
         return value;
      }

      for (int i = next - 1; i >= 0; i--)
      {
         value = actions.get(i).getConfig(key);
         if (!Utils.empty(value))
         {
            return value;
         }
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
         action.run(service, request.getApi(), request.getEndpoint(), this, request, response);
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
      return request.getApi();
   }

   public Endpoint getEndpoint()
   {
      return request.getEndpoint();
   }

   public List<Action> getActions()
   {
      return new ArrayList(actions);
   }

   public Chain withActions(List<Action> actions)
   {
      this.actions.clear();
      for (Action action : actions)
      {
         withAction(action);
      }
      return this;
   }

   public Chain withAction(Action action)
   {
      if (action != null && !actions.contains(action))
         actions.add(action);

      return this;
   }

   public Request request()
   {
      return request;
   }

   //   public Request getRequest()
   //   {
   //      return peek().request;
   //   }
   //
   //   public Response getResponse()
   //   {
   //      return response;
   //   }

}
