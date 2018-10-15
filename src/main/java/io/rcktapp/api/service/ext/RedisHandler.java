package io.rcktapp.api.service.ext;

import java.time.Duration;

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
 * @author kfrankic
 *
 */
public class RedisHandler implements Handler
{
   Logger         log               = LoggerFactory.getLogger(RedisHandler.class);

   private String host              = "";
   private int    port              = 0;

   // times in milliseconds
   private int    readSocketTimeout = 0;
   
   final JedisPoolConfig poolConfig = buildPoolConfig();
   JedisPool jedisPool = null;
   
   // TODO shut down the pool when closing the app


   // TODO should there be a pool of jedis?
//   private Jedis  jedis             = null;

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
//      init();
      if (jedisPool == null)
         jedisPool = new JedisPool(poolConfig, host, port, readSocketTimeout);

      Jedis jedis = jedisPool.getResource();
      
      String key = "foo1";

      // TODO verify connection hasnt been dropped

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
         // most likely a read socket timeout exception
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
            jedis.set(key, chain.getResponse().getJson().toString(), "NX", "EX", 15552000L);
            // jedis.setex(key, 12345, req.getJson().toString());
            
            if (jedis != null)
            jedis.close();
         }
         else
         {
            res.setStatus(SC.SC_200_OK);
            res.setJson(resJson);
         }

      }
      
      

      // TODO should a connect/disconnect occur for every transaction or would it be better
      // to maintain the one connection indefinitely?
      //jedis.disconnect();
      //jedis.quit();
      //jedisPool.close();

   }

//   private void init()
//   {
//      if (jedis == null)
//      {
//         jedis = new Jedis(host, port, connectionTimeout, readSocketTimeout);
//         jedis.connect();
//      }
//
//   }
   
    
   private JedisPoolConfig buildPoolConfig() {
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
