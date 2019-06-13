/**
 * 
 */
package io.rocketpartners.cloud.action.redis;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.rocketpartners.cloud.action.security.AuthSessionCache;
import io.rocketpartners.cloud.model.User;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * This is to be used with the AuthHandler to provide a central session cache, which 
 * is needed when running Snooze on multiple instances behind a load balancer
 * 
 * @author tc-rocket
 */
public class RedisAuthSessionCache implements AuthSessionCache
{
   Logger            log                                = LoggerFactory.getLogger(RedisAuthSessionCache.class);

   ObjectMapper      om                                 = new ObjectMapper();

   // configurable snooze.props 
   protected String  redisHost                          = null;
   protected int     redisPort                          = 6379;

   protected int     redisPoolMin                       = 16;
   protected int     redisPoolMax                       = 128;
   protected boolean redisTestOnBorrow                  = true;
   protected boolean redisTestOnReturn                  = true;
   protected boolean redisTestWhileIdle                 = true;
   protected int     redisMinEvictableIdleTimeMillis    = 60000;
   protected int     redisTimeBetweenEvictionRunsMillis = 30000;
   protected int     redisNumTestsPerEvictionRun        = 3;
   protected boolean redisBlockWhenExhausted            = true;
   protected int     redisReadSocketTimeout             = 2500;
   protected int     redisTtl                           = 8 * 60 * 60;                                         // 8 hours in seconds

   protected String  keyPrefix                          = "RedisAuthSess-";

   JedisPool         jedisPool;

   @Override
   public User get(String sessionKey)
   {
      return (User) execute(new JedisCallback()
         {
            public Object doWithJedis(Jedis jedis) throws Exception
            {
               User user = null;
               String userJson = jedis.get(key(sessionKey));
               if (userJson != null)
               {
                  // reset the ttl after a successful get call
                  jedis.expire(key(sessionKey), redisTtl);
                  user = om.readValue(userJson, User.class);
               }
               return user;
            }
         });
   }

   @Override
   public void put(String sessionKey, User user)
   {
      if (sessionKey != null && user != null)
      {
         execute(new JedisCallback()
            {
               public Object doWithJedis(Jedis jedis) throws Exception
               {
                  String userJson = om.writeValueAsString(user);
                  jedis.setex(key(sessionKey), redisTtl, userJson);
                  return null;
               }
            });
      }
   }

   @Override
   public void remove(String sessionKey)
   {
      execute(new JedisCallback()
         {
            public Object doWithJedis(Jedis jedis) throws Exception
            {
               jedis.del(key(sessionKey));
               return null;
            }
         });
   }

   String key(String sessionKey)
   {
      return keyPrefix + sessionKey;
   }

   Object execute(JedisCallback jedisCallback)
   {
      Jedis jedis = null;
      Object returnVal = null;

      try
      {
         try
         {
            jedis = getJedis();
            returnVal = jedisCallback.doWithJedis(jedis);
         }
         catch (Exception ex)
         {
            log.warn("Error getting or using the Redis client", ex);
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

      return returnVal;
   }

   interface JedisCallback
   {
      public Object doWithJedis(Jedis jedis) throws Exception;
   }

   Jedis getJedis()
   {
      if (jedisPool == null)
      {
         synchronized (this)
         {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(this.redisPoolMax);
            poolConfig.setMaxIdle(this.redisPoolMax);
            poolConfig.setMinIdle(this.redisPoolMin);
            poolConfig.setTestOnBorrow(this.redisTestOnBorrow);
            poolConfig.setTestOnReturn(this.redisTestOnReturn);
            poolConfig.setTestWhileIdle(this.redisTestWhileIdle);
            poolConfig.setMinEvictableIdleTimeMillis(this.redisMinEvictableIdleTimeMillis);
            poolConfig.setTimeBetweenEvictionRunsMillis(this.redisTimeBetweenEvictionRunsMillis);
            poolConfig.setNumTestsPerEvictionRun(this.redisNumTestsPerEvictionRun);
            poolConfig.setBlockWhenExhausted(this.redisBlockWhenExhausted);

            jedisPool = new JedisPool(poolConfig, this.redisHost, this.redisPort, this.redisReadSocketTimeout);
         }
      }

      return jedisPool.getResource();
   }

   public void setRedisHost(String redisHost)
   {
      this.redisHost = redisHost;
   }
   
   public RedisAuthSessionCache withRedisHost(String redisHost) 
   {
      setRedisHost(redisHost);
      return this;
   }

   public void setRedisPort(int redisPort)
   {
      this.redisPort = redisPort;
   }
   
   public RedisAuthSessionCache withRedisPort(int redisPort)
   {
      setRedisPort(redisPort);
      return this;
   }

   public void setRedisPoolMin(int redisPoolMin)
   {
      this.redisPoolMin = redisPoolMin;
   }
   
   public RedisAuthSessionCache withRedisPoolMin(int redisPoolMin)
   {
      setRedisPoolMin(redisPoolMin);
      return this;
   }

   public void setRedisPoolMax(int redisPoolMax)
   {
      this.redisPoolMax = redisPoolMax;
   }
   
   public RedisAuthSessionCache withRedisPoolMax(int redisPoolMax)
   {
      setRedisPoolMax(redisPoolMax);
      return this;
   }

   public void setRedisTestOnBorrow(boolean redisTestOnBorrow)
   {
      this.redisTestOnBorrow = redisTestOnBorrow;
   }
   
   public RedisAuthSessionCache withRedisTestOnBorrow(boolean redisTestOnBorrow)
   {
      setRedisTestOnBorrow(redisTestOnBorrow);
      return this;
   }

   public void setRedisTestOnReturn(boolean redisTestOnReturn)
   {
      this.redisTestOnReturn = redisTestOnReturn;
   }
   
   public RedisAuthSessionCache withRedisTestOnReturn(boolean redisTestOnReturn)
   {
      setRedisTestOnReturn(redisTestOnReturn);
      return this;
   }

   public void setRedisTestWhileIdle(boolean redisTestWhileIdle)
   {
      this.redisTestWhileIdle = redisTestWhileIdle;
   }
   
   public RedisAuthSessionCache withRedisTestWhileIdle(boolean redistTestWhileIdle)
   {
      setRedisTestWhileIdle(redisTestWhileIdle);
      return this;
   }

   public void setRedisMinEvictableIdleTimeMillis(int redisMinEvictableIdleTimeMillis)
   {
      this.redisMinEvictableIdleTimeMillis = redisMinEvictableIdleTimeMillis;
   }
   
   public RedisAuthSessionCache withRedisMinEvictableIdleTimeMillis(int redisMinEvictableIdleTimeMillis)
   {
      setRedisMinEvictableIdleTimeMillis(redisMinEvictableIdleTimeMillis);
      return this;
   }

   public void setRedisTimeBetweenEvictionRunsMillis(int redisTimeBetweenEvictionRunsMillis)
   {
      this.redisTimeBetweenEvictionRunsMillis = redisTimeBetweenEvictionRunsMillis;
   }
   
   public RedisAuthSessionCache withRedisTimeBetweenEvictionRunsMillis(int redisTimeBetweenEvictionRunsMillis)
   {
      setRedisTimeBetweenEvictionRunsMillis(redisTimeBetweenEvictionRunsMillis);
      return this;
   }

   public void setRedisNumTestsPerEvictionRun(int redisNumTestsPerEvictionRun)
   {
      this.redisNumTestsPerEvictionRun = redisNumTestsPerEvictionRun;
   }
   
   public RedisAuthSessionCache withRedisNumTestsPerEvictionRun(int redisNumTestsPerEvictionRun)
   {
      setRedisNumTestsPerEvictionRun(redisNumTestsPerEvictionRun);
      return this;
   }

   public void setRedisBlockWhenExhausted(boolean redisBlockWhenExhausted)
   {
      this.redisBlockWhenExhausted = redisBlockWhenExhausted;
   }
   
   public RedisAuthSessionCache withRedisBlockWhenExhausted(boolean redisBlockWhenExhausted)
   {
      setRedisBlockWhenExhausted(redisBlockWhenExhausted);
      return this;
   }

   public void setRedisReadSocketTimeout(int redisReadSocketTimeout)
   {
      this.redisReadSocketTimeout = redisReadSocketTimeout;
   }
   
   public RedisAuthSessionCache withRedisReadSocketTimeout(int redisReadSocketTimeout)
   {
      setRedisReadSocketTimeout(redisReadSocketTimeout);
      return this;
   }

   public void setRedisTtl(int redisTtl)
   {
      this.redisTtl = redisTtl;
   }
   
   public RedisAuthSessionCache withRedisTtl(int redisTtl)
   {
      setRedisTtl(redisTtl);
      return this;
   }

   public void setKeyPrefix(String keyPrefix)
   {
      this.keyPrefix = keyPrefix;
   }
   
   public RedisAuthSessionCache withKeyPrefix(String keyPrefix)
   {
      setKeyPrefix(keyPrefix);
      return this;
   }

}
