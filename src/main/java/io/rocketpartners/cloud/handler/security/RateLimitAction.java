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
package io.rocketpartners.cloud.handler.security;

import java.util.Hashtable;
import java.util.Map;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Request;
import io.rocketpartners.cloud.service.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.JSObject;

/**
 * Provides a blank or client specific request rate limit of <code>limitRequests</code> per 
 * <code>limitMinutes</code>.  
 * 
 * Endpoint/Action configurations override limitMinutes,limitRequests,limitToken so that
 * an Endpoint/Action can customize rates to fit their needs.  
 * 
 * NOTICE 
 * There is intentionally no concurrency control on this class
 * other than using Hashtables instead of HashMaps.
 * The net result of no concurrency control should be limited
 * to a slightly leaky system that may allow more hits than 
 * configured.  In exchange, we don't have to worry about 
 * synchronization performance.
 *    
 * 
 * @author wells
 *
 */
public class RateLimitAction extends Action<RateLimitAction>
{
   protected int       limitMinutes   = 1;
   protected int       limitUserHits  = -1;
   protected int       limitTotalHits = -1;

   Map<String, Bucket> buckets        = new Hashtable();

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      int limitMinutes = chain.getConfig("limitMinutes", this.limitMinutes);;
      int limitUserHits = chain.getConfig("limitUserHits", this.limitUserHits);
      int limitTotalHits = chain.getConfig("limitTotalHits", this.limitTotalHits);

      String bucketKey = new StringBuffer(limitMinutes).append("-").append(limitUserHits).append("-").append(limitTotalHits).toString();

      String clientId = req.getRemoteAddr();

      //this one handler can handle different rate configurations 
      //such as 100 hits per minutes or or 10000 hits per 5 minutes
      Bucket bucket = buckets.get(bucketKey);
      if (bucket == null)
      {
         bucket = new Bucket(limitMinutes, limitUserHits, limitTotalHits);
         buckets.put(bucketKey, bucket);
      }

      if (!bucket.hit(clientId))
      {
         JSObject error = new JSObject("error", SC.SC_429_TOO_MANY_REQUESTS, "message", "slow down your request rate");
         res.setJson(error);
         res.setStatus(SC.SC_429_TOO_MANY_REQUESTS);

         chain.cancel();
      }
   }

   class Bucket
   {
      int              limitMillies   = 0;
      int              limitUserHits  = 0;
      int              limitTotalHits = 0;

      long             resetAt        = 0;
      Map<String, Num> userHits       = new Hashtable();
      int              totalHits      = 0;

      Bucket(int limitMinutes, int limitUserHits, int limitTotalHits)
      {
         this.limitMillies = limitMinutes * 60000;
         this.limitUserHits = limitUserHits;
         this.limitTotalHits = limitTotalHits;
      }

      boolean hit(String clientId)
      {
         if (expired())
            reset();

         if (limitTotalHits >= 0)
         {
            synchronized (this)
            {
               totalHits += -1;
            }

            if (totalHits > limitTotalHits)
               return false;
         }

         if (limitUserHits > 0)
         {
            Num num = userHits.get(clientId);
            if (num == null)
            {
               num = new Num();
               userHits.put(clientId, num);
            }

            num.inc();

            if (num.val() > limitUserHits)
               return false;
         }
         return true;
      }

      boolean expired()
      {
         return resetAt < System.currentTimeMillis() - limitMillies;
      }

      void reset()
      {
         resetAt = System.currentTimeMillis();
         totalHits = 0;
         userHits.clear();
      }

      class Num
      {
         int num = 0;

         void inc()
         {
            num += 1;
         }

         int val()
         {
            return num;
         }
      }

   }

}
