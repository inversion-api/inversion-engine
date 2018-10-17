package io.rcktapp.api.service.ext;

import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.forty11.web.js.JS;
import io.forty11.web.js.JSObject;
import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.SC;
import io.rcktapp.api.service.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * The service builds a key from the request url & parameters.  If the key does not exist within Redis,
 * the request is passed along to the GetHandler.  The response from the GetHandler will be inserted
 * into Redis with an expiration.
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
public class RedisHandler implements Handler
{
   Logger                log               = LoggerFactory.getLogger(RedisHandler.class);

   // configurable snooze.props 
   private String        host              = "";
   private int           port              = 6379;
   private String        skipCache         = "nocache";
   private int           readSocketTimeout = 2500;                                       // time in milliseconds
   private int           ttl               = 15552000;                                   // time to live 15,552,000s == 180 days

   final JedisPoolConfig poolConfig        = buildPoolConfig();
   JedisPool             jedisPool         = null;

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      if (jedisPool == null)
         jedisPool = new JedisPool(poolConfig, host, port, readSocketTimeout);

      Jedis jedis = jedisPool.getResource();

      // the key is derived from the URL
      String key = removeUrlProtocol(req.getApiUrl()) + req.getPath() + sortRequestParameters(req.getParams());
      log.info("key: " + key);

      // request should include a json object
      JSObject resJson = null;

      try
      {
         String value = null;

         // attempt to get the value from Redis
         if (!req.getParams().containsKey(skipCache))
            value = jedis.get(key);

         if (value != null)
            resJson = JS.toJSObject(value);

      }
      catch (Exception e)
      {
         // most likely a read socket timeout exception... log it and move on.
         log.warn("Failed to retrieve from Redis the key: " + key, e);
      }
      finally
      {
         if (resJson == null)
         {
            chain.go();

            // TODO should the naming convention include the TTL in the name?

            // see class header for explanation on setex()  
            // jedis.set(key, chain.getResponse().getJson().toString(), setParams().ex(ttl));
            jedis.setex(key, ttl, chain.getResponse().getJson().toString());

            if (jedis != null)
               jedis.close();
         }
         else
         {
            res.setStatus(SC.SC_200_OK);
            res.setJson(resJson);
         }

      }

      // TODO close down the pool on shutdown
      //jedisPool.close();

   }

   /**
    * Sorts the request parameters alphabetically
    * @param requestParamMap map representing the request parameters
    * @return a concatenated string of each param beginning with '?' and joined by '&'
    */
   private String sortRequestParameters(Map<String, String> requestParamMap)
   {
      TreeMap<String, String> sortedKeyMap = new TreeMap<>(requestParamMap);

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

      return sortedParams;
   }

   private String removeUrlProtocol(String url)
   {
      return url.substring(url.indexOf("://") + 3);
   }

   private JedisPoolConfig buildPoolConfig()
   {
      final JedisPoolConfig poolConfig = new JedisPoolConfig();
      poolConfig.setMaxTotal(128);
      poolConfig.setMaxIdle(128);
      poolConfig.setMinIdle(16);
      poolConfig.setTestOnBorrow(true);
      poolConfig.setTestOnReturn(true);
      poolConfig.setTestWhileIdle(true);
      poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
      poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
      poolConfig.setNumTestsPerEvictionRun(3);
      poolConfig.setBlockWhenExhausted(true);
      return poolConfig;
   }

}
