package io.rcktapp.api.service.ext;

import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.forty11.j.J;
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
 * @author kfrankic
 *
 */
public class RedisHandler implements Handler
{
   Logger                log               = LoggerFactory.getLogger(RedisHandler.class);

   private String        host              = "";
   private int           port              = 0;

   // times in milliseconds
   private int           readSocketTimeout = 0;

   final JedisPoolConfig poolConfig        = buildPoolConfig();
   JedisPool             jedisPool         = null;

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      if (jedisPool == null)
         jedisPool = new JedisPool(poolConfig, host, port, readSocketTimeout);

      Jedis jedis = jedisPool.getResource();
      
      // the key is derived from the URL
      String url = removeUrlProtocol(req.getApiUrl()) + req.getPath() + sortRequestParameters(req.getParams());
      String key = J.md5(url.getBytes());
      log.info("url: " + url + " -> key: " + key);

      // request should include a json object
      JSObject resJson = null;

      try
      {
         // attempt to get the value from Redis
         String value = jedis.get(key);

         if (value != null)
            resJson = JS.toJSObject(value);

      }
      catch (Exception e)
      {
         // most likely a read socket timeout exception... log it and move on.
         log.warn("Failed to retrieve from Redis the key: " + key + " url: " + url, e);
      }
      finally
      {
         if (resJson == null)
         {
            // No value found, pass the chain on to the GetHandler

            // Currently, no reason to catch an exception because there will be no data to store in Redis
            chain.go();

            // TODO should the naming convention include the TTL in the name?

            // hash fields can't have an associated time to live (expire) like a real key, and can only contain a string.
            // nxxx NX|XX, NX -- Only set the key if it does not already exist. XX -- Only set the keyif it already exist.
            // expx EX|PX, expire time units: EX = seconds; PX = milliseconds
            // 106,751,991,167,301 days MAX
            // 15,552,000s == 180 days
            // Note: Since the SET command options can replace SETNX, SETEX, PSETEX, it is possible that in future 
            // versions of Redis these three commands will be deprecated and finally removed.
            // A future version of jedis alter's .set() to allow for a SetParams object to be used to set 'ex'
            // without being required to set 'nx'
            // jedis.set(key, chain.getResponse().getJson().toString(), setParams().ex(15552000));
            jedis.setex(key, 15552000, chain.getResponse().getJson().toString());

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
