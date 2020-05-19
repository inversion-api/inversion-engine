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
package io.inversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.collections4.map.CaseInsensitiveMap;

import io.inversion.utils.JSNode;
import io.inversion.utils.Path;
import io.inversion.utils.Utils;

public class Chain
{
   static ThreadLocal<Stack<Chain>> chainLocal = new ThreadLocal();

   public static void resetAll()
   {
      chainLocal = new ThreadLocal();
   }

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

   public static String buildLink(Collection collection, Object resourceKey, String subCollectionKey)
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

      if (req.getCollection() != null && (collection == req.getCollection() || collection.getDb() == req.getCollection().getDb()))//
      {
         //going after the same collection...so must be going after the same endpoint
         //so get the endpoint path from the current request and ame sure it is on the url.

         Path epp = req.getEndpointPath();

         if (epp != null && epp.size() > 0)
         {
            url += epp + "/";
         }
      }
      else if (collection.getDb().getEndpointPath() != null)
      {
         Path epP = collection.getDb().getEndpointPath();

         for (int i = 0; i < epP.size(); i++)
         {
            if (epP.isWildcard(i))
               break;

            if (epP.isVar(i))
            {
               String value = null;
               String name = epP.getVarName(i);
               switch (name.toLowerCase())
               {
                  case "collection":
                     value = collection.getName();
                     break;
                  case "resource":
                     value = resourceKey + "";
                     break;
                  case "relationship":
                     value = subCollectionKey;
                     break;
                  default :
                     value = req.getUrl().getParam(name);
               }
               if (value == null)
                  ApiException.throw500InternalServerError("Unable to determine path for link to collection '{}', resource '{}', relationship '{}'", collection.getName(), resourceKey + "", subCollectionKey + "");

               url += epP.get(i) + "/";
            }
            else
            {
               url += epP.get(i) + "/";
            }
         }

         url += collection.getDb().getEndpointPath() + "/";
      }

      if (!Utils.empty(collectionKey))
      {
         if (!url.endsWith("/"))
            url += "/";

         url += collectionKey;
      }

      if (!Utils.empty(resourceKey))
         url += "/" + resourceKey.toString();

      if (!Utils.empty(subCollectionKey))
         url += "/" + subCollectionKey;

      if (req.getApi().getUrl() != null && !url.startsWith(req.getApi().getUrl()))
      {
         String newUrl = req.getApi().getUrl();
         while (newUrl.endsWith("/"))
            newUrl = newUrl.substring(0, newUrl.length() - 1);

         url = newUrl + url.substring(url.indexOf("/", 8));
      }

      if (req.getApi() != null)
      {
         if (Utils.empty(req.getApi().getUrl()))
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

   //   public static String buildLink(String collectionKey, String resourceKey)
   //   {
   //      Request req = Chain.peek().getRequest();
   //      String url = req.getUrl().toString();
   //      if (url.indexOf("?") >= 0)
   //         url = url.substring(0, url.indexOf("?"));
   //
   //      if (req.getRelationshipKey() != null)
   //      {
   //         url = url.substring(0, url.lastIndexOf("/"));
   //      }
   //
   //      if (req.getResourceKey() != null)
   //      {
   //         url = url.substring(0, url.lastIndexOf("/"));
   //      }
   //
   //      if (collectionKey != null && req.getCollectionKey() != null)
   //      {
   //         url = url.substring(0, url.lastIndexOf("/"));
   //      }
   //
   //      if (collectionKey != null)
   //         url += "/" + collectionKey;
   //
   //      if (resourceKey != null)
   //         url += "/" + resourceKey;
   //
   //      if (req.getApi().getUrl() != null && !url.startsWith(req.getApi().getUrl()))
   //      {
   //         String newUrl = req.getApi().getUrl();
   //         while (newUrl.endsWith("/"))
   //            newUrl = newUrl.substring(0, newUrl.length() - 1);
   //
   //         url = newUrl + url.substring(url.indexOf("/", 8));
   //      }
   //
   //      return url;
   //
   //   }

   protected Engine             engine   = null;
   protected List<ActionMatch>  actions  = new ArrayList();
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
         value = actions.get(i).action.getConfig(key);
         if (value != null)
         {
            value = value.toLowerCase();
            values.addAll(Utils.explode(",", value));
         }
      }

      value = request.getUrl().getParam(key);
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
         keys.addAll(actions.get(i).action.getConfigKeys());
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
         value = actions.get(i).action.getConfig(key);
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

   public void go() throws ApiException
   {
      while (next())
      {
         ;
      }
   }

   public boolean next() throws ApiException
   {
      if (!isCanceled() && next < actions.size())
      {
         ActionMatch actionMatch = actions.get(next);
         next += 1;

         Map<String, String> pathParams = new HashMap();
         actionMatch.rule.extract(pathParams, new Path(actionMatch.path));

         Engine.applyPathParams(pathParams, request.getUrl(), request.getJson());

         actionMatch.action.run(request, response);
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

   public List<ActionMatch> getActions()
   {
      return new ArrayList(actions);
   }

   public Chain withActions(List<ActionMatch> actions)
   {
      for (ActionMatch action : actions)
      {
         withAction(action);
      }
      return this;
   }

   public Chain withAction(ActionMatch action)
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

   static class ActionMatch implements Comparable<ActionMatch>
   {
      Path   rule   = null;
      Path   path   = null;
      Action action = null;

      public ActionMatch(Path rule, Path path, Action action)
      {
         super();
         this.rule = rule;
         this.path = path;
         this.action = action;
      }

      @Override
      public int compareTo(ActionMatch o)
      {
         return action.compareTo(o.action);
      }

      public String toString()
      {
         return rule + " " + path + " " + action;
      }
   }

}
