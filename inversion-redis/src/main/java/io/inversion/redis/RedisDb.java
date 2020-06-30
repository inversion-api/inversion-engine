/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
package io.inversion.redis;

import io.inversion.ApiException;
import io.inversion.Collection;
import io.inversion.Db;
import io.inversion.Results;
import io.inversion.rql.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.List;
import java.util.Map;

public class RedisDb extends Db<RedisDb> {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected transient JedisPool jedis = null;

    // configurable props
    protected String host = null;
    protected int    port = 6379;

    protected int     poolMin                       = 16;
    protected int     poolMax                       = 128;
    protected boolean testOnBorrow                  = true;
    protected boolean testOnReturn                  = true;
    protected boolean testWhileIdle                 = true;
    protected int     minEvictableIdleTimeMillis    = 60000;
    protected int     timeBetweenEvictionRunsMillis = 30000;
    protected int     numTestsPerEvictionRun        = 3;
    protected boolean blockWhenExhausted            = true;

    protected String nocacheParam      = "nocache";
    protected int    readSocketTimeout = 2500;                               // time in milliseconds
    protected int    ttl               = 15552000;                           // time to live 15,552,000s == 180 days

    @Override
    public Results doSelect(Collection table, List<Term> queryTerms) throws ApiException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> doUpsert(Collection table, List<Map<String, Object>> rows) throws ApiException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void delete(Collection table, List<Map<String, Object>> indexValues) throws ApiException {
        // TODO Auto-generated method stub

    }

    protected Jedis getRedisClient() {
        if (jedis == null) {
            synchronized (this) {
                if (jedis == null) {
                    JedisPoolConfig poolConfig = new JedisPoolConfig();
                    poolConfig.setMaxTotal(this.poolMax);
                    poolConfig.setMaxIdle(this.poolMax);
                    poolConfig.setMinIdle(this.poolMin);
                    poolConfig.setTestOnBorrow(this.testOnBorrow);
                    poolConfig.setTestOnReturn(this.testOnReturn);
                    poolConfig.setTestWhileIdle(this.testWhileIdle);
                    poolConfig.setMinEvictableIdleTimeMillis(this.minEvictableIdleTimeMillis);
                    poolConfig.setTimeBetweenEvictionRunsMillis(this.timeBetweenEvictionRunsMillis);
                    poolConfig.setNumTestsPerEvictionRun(this.numTestsPerEvictionRun);
                    poolConfig.setBlockWhenExhausted(this.blockWhenExhausted);

                    jedis = new JedisPool(poolConfig, this.host, this.port, this.readSocketTimeout);
                }
            }
        }

        return jedis.getResource();
    }

    public String getHost() {
        return host;
    }

    public RedisDb withHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public RedisDb withPort(int port) {
        this.port = port;
        return this;
    }

    public int getPoolMin() {
        return poolMin;
    }

    public RedisDb withPoolMin(int poolMin) {
        this.poolMin = poolMin;
        return this;
    }

    public int getPoolMax() {
        return poolMax;
    }

    public RedisDb withPoolMax(int poolMax) {
        this.poolMax = poolMax;
        return this;
    }

    public boolean isTestOnBorrow() {
        return testOnBorrow;
    }

    public RedisDb withTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
        return this;
    }

    public boolean isTestOnReturn() {
        return testOnReturn;
    }

    public RedisDb withTestOnReturn(boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
        return this;
    }

    public boolean isTestWhileIdle() {
        return testWhileIdle;
    }

    public RedisDb withTestWhileIdle(boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
        return this;
    }

    public int getMinEvictableIdleTimeMillis() {
        return minEvictableIdleTimeMillis;
    }

    public RedisDb withMinEvictableIdleTimeMillis(int minEvictableIdleTimeMillis) {
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
        return this;
    }

    public int getTimeBetweenEvictionRunsMillis() {
        return timeBetweenEvictionRunsMillis;
    }

    public RedisDb withTimeBetweenEvictionRunsMillis(int timeBetweenEvictionRunsMillis) {
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
        return this;
    }

    public int getNumTestsPerEvictionRun() {
        return numTestsPerEvictionRun;
    }

    public RedisDb withNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        this.numTestsPerEvictionRun = numTestsPerEvictionRun;
        return this;
    }

    public boolean isBlockWhenExhausted() {
        return blockWhenExhausted;
    }

    public RedisDb withBlockWhenExhausted(boolean blockWhenExhausted) {
        this.blockWhenExhausted = blockWhenExhausted;
        return this;
    }

    public String getNocacheParam() {
        return nocacheParam;
    }

    public RedisDb withNocacheParam(String nocacheParam) {
        this.nocacheParam = nocacheParam;
        return this;
    }

    public int getReadSocketTimeout() {
        return readSocketTimeout;
    }

    public RedisDb withReadSocketTimeout(int readSocketTimeout) {
        this.readSocketTimeout = readSocketTimeout;
        return this;
    }

    public int getTtl() {
        return ttl;
    }

    public RedisDb withTtl(int ttl) {
        this.ttl = ttl;
        return this;
    }

}
