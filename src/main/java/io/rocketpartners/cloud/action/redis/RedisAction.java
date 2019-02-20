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
package io.rocketpartners.cloud.action.redis;

import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Request;
import io.rocketpartners.cloud.service.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Utils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

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
   Logger                       log                                = LoggerFactory.getLogger(getClass());

   // configurable snooze.props 
   protected String             redisHost                          = null;
   protected int                redisPort                          = 6379;

   protected int                redisPoolMin                       = 16;
   protected int                redisPoolMax                       = 128;
   protected boolean            redisTestOnBorrow                  = true;
   protected boolean            redisTestOnReturn                  = true;
   protected boolean            redisTestWhileIdle                 = true;
   protected int                redisMinEvictableIdleTimeMillis    = 60000;
   protected int                redisTimeBetweenEvictionRunsMillis = 30000;
   protected int                redisNumTestsPerEvictionRun        = 3;
   protected boolean            redisBlockWhenExhausted            = true;

   protected String             redisNocacheParam                  = "nocache";
   protected int                redisReadSocketTimeout             = 2500;                               // time in milliseconds
   protected int                redisTtl                           = 15552000;                           // time to live 15,552,000s == 180 days

   Hashtable<String, JedisPool> pools                              = new Hashtable();

   @Override
   public void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      //caching only makes sense for GET requests
      if (!"GET".equalsIgnoreCase(req.getMethod()))
         return;

      //only cache top level request, on internal recursive requests
      if (chain.getParent() != null)
         return;

      String nocacheParam = chain.getConfig("redisNocacheParam", this.redisNocacheParam);

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
            ObjectNode resJson = null;

            String value = null;
            try
            {
               jedis = getPool(chain).getResource();
               value = jedis.get(key);
            }
            catch (Exception ex)
            {
               log.warn("Failed to retrieve from Redis the key: " + key, ex);
            }

            if (value != null)
            {
               log.debug("CACHE HIT : " + key);

               resJson = Utils.parseJsonObject(value);
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
               
               ObjectNode json = res.getJson();

               if (res.getStatusCode() == 200 && json != null && json.getProperties().size() > 0)
               {
                  // will NOT store empty JSON responses
                  try
                  {
                     int ttl = chain.getConfig("redisTtl", this.redisTtl);
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

   JedisPool getPool(Chain chain)
   {
      String host = chain.getConfig("redisHost", this.redisHost);
      int port = chain.getConfig("redisPort", this.redisPort);

      String poolKey = chain.getConfig("redisPoolKey", host + ":" + port);

      JedisPool jedis = pools.get(poolKey);
      if (jedis == null)
      {
         synchronized (this)
         {
            jedis = pools.get(poolKey);
            if (jedis == null)
            {
               JedisPoolConfig poolConfig = new JedisPoolConfig();
               poolConfig.setMaxTotal(chain.getConfig("redisPoolMax", this.redisPoolMax));
               poolConfig.setMaxIdle(chain.getConfig("redisPoolMax", this.redisPoolMax));
               poolConfig.setMinIdle(chain.getConfig("redisPoolMin", this.redisPoolMin));
               poolConfig.setTestOnBorrow(chain.getConfig("redisTestOnBorrow", this.redisTestOnBorrow));
               poolConfig.setTestOnReturn(chain.getConfig("redisTestOnReturn", this.redisTestOnReturn));
               poolConfig.setTestWhileIdle(chain.getConfig("redisTestWhileIdle", this.redisTestWhileIdle));
               poolConfig.setMinEvictableIdleTimeMillis(chain.getConfig("redisMinEvictableIdleTimeMillis", this.redisMinEvictableIdleTimeMillis));
               poolConfig.setTimeBetweenEvictionRunsMillis(chain.getConfig("redisTimeBetweenEvictionRunsMillis", this.redisTimeBetweenEvictionRunsMillis));
               poolConfig.setNumTestsPerEvictionRun(chain.getConfig("redisNumTestsPerEvictionRun", this.redisNumTestsPerEvictionRun));
               poolConfig.setBlockWhenExhausted(chain.getConfig("redisBlockWhenExhausted", this.redisBlockWhenExhausted));

               jedis = new JedisPool(poolConfig, host, port, chain.getConfig("redisReadSocketTimeout", this.redisReadSocketTimeout));
               pools.put(poolKey, jedis);
            }
         }
      }

      return jedis;
   }

}
