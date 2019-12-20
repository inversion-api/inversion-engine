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
package io.inversion.cloud.action.redis;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;
import redis.clients.jedis.Jedis;

/**
 * The service builds a key from the request url & parameters.  If the key does not exist within Redis,
 * the request is passed along to the GetHandler.  The JSON response from the GetHandler will be inserted
 * into Redis with an expiration if the JSON is not null or empty.
 * 
 * The initial Redis check can be bypassed by including the skipCache (verify value below) request parameter. 
 * 
 * The current implementation of Jedis.set() does not allow clobbering a key/value & expiration but will in 
 * a future build. Because of that, Jedis.setex() is used.  Since the SET command options can replace SETNX, 
 * SETEX, PSETEX, it is possible that in future versions of Redis these three commands will be deprecated 
 * and finally removed.
 * 
 * Jedis.set() parameter explanation...
 * nxxx NX|XX, NX -- Only set the key if it does not already exist. XX -- Only set the key if it already exist.
 * expx EX|PX, expire time units: EX = seconds; PX = milliseconds
 
 * A future version of jedis alter's .set() to allow for a SetParams object to be used to set 'ex'
 * without requiring the setting of 'nx' 
 * 
 * @author kfrankic
 *
 */
public class RedisAction extends Action<RedisAction>
{
   protected transient Logger log = LoggerFactory.getLogger(getClass());

   @Override
   public void run(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      //caching only makes sense for GET requests
      if (!"GET".equalsIgnoreCase(req.getMethod()))
         return;

      //only cache top level request, on internal recursive requests
      if (chain.getParent() != null)
         return;

      RedisDb db = (RedisDb) req.getCollection().getDb();

      String nocacheParam = chain.getConfig("redisNocacheParam", db.getNocacheParam());

      // remove this param before creating the key, so this param is not included in the key
      boolean skipCache = (req.removeParam(nocacheParam) != null);

      if (skipCache)
         return;

      Jedis jedis = null;

      try
      {
         if (!skipCache)
         {
            // the key is derived from the URL
            String key = getCacheKey(chain);

            // request should include a json object
            JSNode resJson = null;

            String value = null;
            try
            {
               jedis = db.getRedisClient();
               value = jedis.get(key);
            }
            catch (Exception ex)
            {
               log.warn("Failed to retrieve from Redis the key: " + key, ex);
            }

            if (value != null)
            {
               log.debug("CACHE HIT : " + key);

               resJson = JSNode.parseJsonNode(value);
               res.withJson(resJson);
               res.withStatus(SC.SC_200_OK);
               chain.cancel();
            }
            else
            {
               log.debug("CACHE MISS: " + key);

               chain.go();

               // TODO should the naming convention include the TTL in the name?

               // see class header for explanation on setex()  
               // jedis.set(key, chain.getResponse().getJson().toString(), setParams().ex(ttl));

               JSNode json = res.getJson();

               if (res.getStatusCode() == 200 && json != null && json.getProperties().size() > 0)
               {
                  // will NOT store empty JSON responses
                  try
                  {
                     int ttl = chain.getConfig("redisTtl", db.getTtl());
                     jedis.setex(key, ttl, json.toString());
                  }
                  catch (Exception ex)
                  {
                     log.warn("Failed to save Redis key: " + key, ex);
                  }
               }
            }
         }
      }
      finally
      {
         if (jedis != null)
         {
            try
            {
               jedis.close();
            }
            catch (Exception ex)
            {
               log.warn("Error closing redis connection", ex);
            }
         }
      }
   }

   /**
    * Sorts the request parameters alphabetically
    * @param requestParamMap map representing the request parameters
    * @return a concatenated string of each param beginning with '?' and joined by '&'
    */
   String getCacheKey(Chain chain)
   {
      TreeMap<String, String> sortedKeyMap = new TreeMap<>(chain.getRequest().getParams());

      String sortedParams = "";

      boolean isFirstParam = true;

      for (Map.Entry<String, String> entry : sortedKeyMap.entrySet())
      {
         if (isFirstParam)
         {
            sortedParams += "?";
            isFirstParam = false;
         }
         else
            sortedParams += "&";

         sortedParams += entry.getKey();

         if (!entry.getValue().isEmpty())
         {
            sortedParams += "=" + entry.getValue();
         }

      }

      String key = chain.getRequest().getApiUrl();
      key = key.substring(key.indexOf("://") + 3);
      key += chain.getRequest().getPath();
      key += sortedParams;

      return key;
   }

}
