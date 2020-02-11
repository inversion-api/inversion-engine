/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.collections4.map.CaseInsensitiveMap;

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.Path;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.User;
import io.inversion.cloud.utils.Utils;

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

   public static Chain first()
   {
      Stack<Chain> stack = get();
      if (!stack.empty())
      {
         return stack.get(0);
      }
      return null;
   }

   public static Chain peek()
   {
      Stack<Chain> stack = get();
      if (!stack.empty())
         return stack.peek();
      return null;

   }

   public static Chain push(Engine engine, Request req, Response res)
   {
      Chain child = new Chain(engine, req, res);

      Chain parent = peek();
      if (parent != null)
         child.setParent(parent);

      req.withChain(child);
      get().push(child);

      return child;
   }

   public static Chain pop()
   {
      return get().pop();
   }

   public static User getUser()
   {
      Chain chain = peek();
      if (chain != null)
      {
         do
         {
            if (chain.user != null)
               return chain.user;
         }
         while ((chain = chain.parent) != null);
      }
      return null;
   }

   public static int size()
   {
      return get().size();
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
      Request req = peek().getRequest();

      String collectionKey = collection.getName();

      if (req.getCollection() == collection)
         collectionKey = req.getCollectionKey();

      String url = req.getApiUrl();

      if (url == null)
         url = "";

      if (!url.endsWith("/"))
         url += "/";

      if (collection == req.getCollection())
      {
         //going after the same collection...so must be going after the same endpoint
         //so get the endpoint path from the current request and ame sure it is on the url.

         Path epp = req.getEndpointPath();

         if (epp != null && epp.size() > 0)
         {
            url += epp + "/";
         }
      }
      else if (collection != null && collection.getIncludePaths().size() > 0)
      {
         String collectionPath = (String) collection.getIncludePaths().get(0).toString();
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
         url += "/" + entityKey.toString();

      if (!Utils.empty(subCollectionKey))
         url += "/" + subCollectionKey;

      if (req.getApi().getUrl() != null && !url.startsWith(req.getApi().getUrl()))
      {
         String newUrl = req.getApi().getUrl();
         while (newUrl.endsWith("/"))
            newUrl = newUrl.substring(0, newUrl.length() - 1);

         url = newUrl + url.substring(url.indexOf("/", 8));
      }

      if (collection.getApi() != null)
      {
         if (Utils.empty(collection.getApi().getUrl()))
         {
            String proto = req.getHeader("x-forwarded-proto");
            if (!Utils.empty(proto))
            {
               url = proto + url.substring(url.indexOf(':'), url.length());
            }
         }
      }
      return url;
   }

   public static String buildLink(String collectionKey, String entityKey)
   {
      Request req = Chain.peek().getRequest();
      String url = req.getUrl().toString();
      if (url.indexOf("?") >= 0)
         url = url.substring(0, url.indexOf("?"));

      if (req.getSubCollectionKey() != null)
      {
         url = url.substring(0, url.lastIndexOf("/"));
      }

      if (req.getEntityKey() != null)
      {
         url = url.substring(0, url.lastIndexOf("/"));
      }

      if (collectionKey != null && req.getCollectionKey() != null)
      {
         url = url.substring(0, url.lastIndexOf("/"));
      }

      if (collectionKey != null)
         url += "/" + collectionKey;

      if (entityKey != null)
         url += "/" + entityKey;

      if (req.getApi().getUrl() != null && !url.startsWith(req.getApi().getUrl()))
      {
         String newUrl = req.getApi().getUrl();
         while (newUrl.endsWith("/"))
            newUrl = newUrl.substring(0, newUrl.length() - 1);

         url = newUrl + url.substring(url.indexOf("/", 8));
      }

      return url;

   }

   protected Engine             engine   = null;
   protected List<Action>       actions  = new ArrayList();
   protected Request            request  = null;
   protected Response           response = null;

   protected int                next     = 0;
   protected boolean            canceled = false;

   protected User               user     = null;

   protected CaseInsensitiveMap vars     = new CaseInsensitiveMap();

   protected Chain              parent   = null;

   private Chain(Engine engine, Request req, Response res)
   {
      this.engine = engine;
      this.request = req;
      this.response = res;
   }

   public Chain withUser(User user)
   {
      this.user = user;
      return this;
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
      List<String> values = new LinkedList();

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

      for (int i = 0; i < values.size(); i++)
      {
         value = values.get(i);
         value = Utils.dequote(value);
         values.set(i, value);
      }

      return new LinkedHashSet(values);
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
      if (request == null)
      {
         System.out.println("The Request on the Chain is null, this should never happen");
      }
      else if (request.getEndpoint() == null)
      {
         System.out.println("The Endpoint on the Request is null, this should never happen");
         System.out.println(" -- Chain stack starting with this chain and then every parent after");

         Chain tempChain = this;
         while (tempChain != null)
         {
            System.out.println(" ----  " + tempChain + " ::: " + tempChain.getRequest() + " ::: " + tempChain.getRequest().getUrl());
            tempChain = tempChain.getParent();
         }
      }

      String value = null;

      for (int i = next - 1; i >= 0; i--)
      {
         value = actions.get(i).getConfig(key);
         if (!Utils.empty(value))
         {
            return value;
         }
      }

      value = request.getEndpoint().getConfig(key);
      if (!Utils.empty(value))
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
         action.run(request, response);
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

   public Engine getEngine()
   {
      return engine;
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

   public Request getRequest()
   {
      return request;
   }

   public Response getResponse()
   {
      return response;
   }

}
