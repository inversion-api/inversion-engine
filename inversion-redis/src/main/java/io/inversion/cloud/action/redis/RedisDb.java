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

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inversion.cloud.model.Db;
import io.inversion.cloud.model.Results;
import io.inversion.cloud.model.Table;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.utils.Rows.Row;
import io.inversion.cloud.utils.Utils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisDb extends Db<RedisDb>
{
   protected transient Logger    log                           = LoggerFactory.getLogger(getClass());

   protected transient JedisPool jedis                         = null;

   // configurable props 
   protected String              host                          = null;
   protected int                 port                          = 6379;

   protected int                 poolMin                       = 16;
   protected int                 poolMax                       = 128;
   protected boolean             testOnBorrow                  = true;
   protected boolean             testOnReturn                  = true;
   protected boolean             testWhileIdle                 = true;
   protected int                 minEvictableIdleTimeMillis    = 60000;
   protected int                 timeBetweenEvictionRunsMillis = 30000;
   protected int                 numTestsPerEvictionRun        = 3;
   protected boolean             blockWhenExhausted            = true;

   protected String              nocacheParam                  = "nocache";
   protected int                 readSocketTimeout             = 2500;                               // time in milliseconds
   protected int                 ttl                           = 15552000;                           // time to live 15,552,000s == 180 days

   @Override
   public Results<Row> select(Table table, List<Term> columnMappedTerms) throws Exception
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void delete(Table table, String entityKey) throws Exception
   {
      // TODO Auto-generated method stub

   }

   @Override
   public String upsert(Table table, Map<String, Object> row) throws Exception
   {
      // TODO Auto-generated method stub
      return null;
   }

   protected Jedis getRedisClient()
   {
      if (jedis == null)
      {
         synchronized (this)
         {
            if (jedis == null)
            {
               JedisPoolConfig poolConfig = new JedisPoolConfig();
               poolConfig.setMaxTotal(Utils.findSysEnvPropInt(getName() + ".poolMax", this.poolMax));
               poolConfig.setMaxIdle(Utils.findSysEnvPropInt(getName() + ".poolMax", this.poolMax));
               poolConfig.setMinIdle(Utils.findSysEnvPropInt(getName() + ".poolMin", this.poolMin));
               poolConfig.setTestOnBorrow(Utils.findSysEnvPropBool(getName() + "testOnBorrow", this.testOnBorrow));
               poolConfig.setTestOnReturn(Utils.findSysEnvPropBool(getName() + ".testOnReturn", this.testOnReturn));
               poolConfig.setTestWhileIdle(Utils.findSysEnvPropBool(getName() + ".testWhileIdle", this.testWhileIdle));
               poolConfig.setMinEvictableIdleTimeMillis(Utils.findSysEnvPropInt(getName() + ".minEvictableIdleTimeMillis", this.minEvictableIdleTimeMillis));
               poolConfig.setTimeBetweenEvictionRunsMillis(Utils.findSysEnvPropInt(getName() + ".timeBetweenEvictionRunsMillis", this.timeBetweenEvictionRunsMillis));
               poolConfig.setNumTestsPerEvictionRun(Utils.findSysEnvPropInt(getName() + ".numTestsPerEvictionRun", this.numTestsPerEvictionRun));
               poolConfig.setBlockWhenExhausted(Utils.findSysEnvPropBool(getName() + ".blockWhenExhausted", this.blockWhenExhausted));

               String host = Utils.findSysEnvPropStr(getName() + ".host", this.host);
               int port = Utils.findSysEnvPropInt(getName() + ".port", this.port);
               int timeout = Utils.findSysEnvPropInt(getName() + ".readSocketTimeout", this.readSocketTimeout);

               jedis = new JedisPool(poolConfig, host, port, timeout);
            }
         }
      }

      return jedis.getResource();
   }

   public String getHost()
   {
      return host;
   }

   public RedisDb withHost(String host)
   {
      this.host = host;
      return this;
   }

   public int getPort()
   {
      return port;
   }

   public RedisDb withPort(int port)
   {
      this.port = port;
      return this;
   }

   public int getPoolMin()
   {
      return poolMin;
   }

   public RedisDb withPoolMin(int poolMin)
   {
      this.poolMin = poolMin;
      return this;
   }

   public int getPoolMax()
   {
      return poolMax;
   }

   public RedisDb withPoolMax(int poolMax)
   {
      this.poolMax = poolMax;
      return this;
   }

   public boolean isTestOnBorrow()
   {
      return testOnBorrow;
   }

   public RedisDb withTestOnBorrow(boolean testOnBorrow)
   {
      this.testOnBorrow = testOnBorrow;
      return this;
   }

   public boolean isTestOnReturn()
   {
      return testOnReturn;
   }

   public RedisDb withTestOnReturn(boolean testOnReturn)
   {
      this.testOnReturn = testOnReturn;
      return this;
   }

   public boolean isTestWhileIdle()
   {
      return testWhileIdle;
   }

   public RedisDb withTestWhileIdle(boolean testWhileIdle)
   {
      this.testWhileIdle = testWhileIdle;
      return this;
   }

   public int getMinEvictableIdleTimeMillis()
   {
      return minEvictableIdleTimeMillis;
   }

   public RedisDb withMinEvictableIdleTimeMillis(int minEvictableIdleTimeMillis)
   {
      this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
      return this;
   }

   public int getTimeBetweenEvictionRunsMillis()
   {
      return timeBetweenEvictionRunsMillis;
   }

   public RedisDb withTimeBetweenEvictionRunsMillis(int timeBetweenEvictionRunsMillis)
   {
      this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
      return this;
   }

   public int getNumTestsPerEvictionRun()
   {
      return numTestsPerEvictionRun;
   }

   public RedisDb withNumTestsPerEvictionRun(int numTestsPerEvictionRun)
   {
      this.numTestsPerEvictionRun = numTestsPerEvictionRun;
      return this;
   }

   public boolean isBlockWhenExhausted()
   {
      return blockWhenExhausted;
   }

   public RedisDb withBlockWhenExhausted(boolean blockWhenExhausted)
   {
      this.blockWhenExhausted = blockWhenExhausted;
      return this;
   }

   public String getNocacheParam()
   {
      return nocacheParam;
   }

   public RedisDb withNocacheParam(String nocacheParam)
   {
      this.nocacheParam = nocacheParam;
      return this;
   }

   public int getReadSocketTimeout()
   {
      return readSocketTimeout;
   }

   public RedisDb withReadSocketTimeout(int readSocketTimeout)
   {
      this.readSocketTimeout = readSocketTimeout;
      return this;
   }

   public int getTtl()
   {
      return ttl;
   }

   public RedisDb withTtl(int ttl)
   {
      this.ttl = ttl;
      return this;
   }

}
