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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.rcktapp.api.handler.security;

import java.util.Hashtable;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.SC;
import io.rcktapp.api.service.Service;

public class RateHandler implements Handler
{
   int              minutes = 1;
   int              hits    = 200;
   long             resetAt = 0;

   Map<String, Num> counts  = new Hashtable();

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

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      if (resetAt < System.currentTimeMillis() - (minutes * 60000))
      {
         synchronized (this)
         {
            if (resetAt < System.currentTimeMillis() - (minutes * 60000))
            {
               counts.clear();
               resetAt = System.currentTimeMillis();
            }
         }
      }

      String ip = getIp(req.getHttpServletRequest());
      String token = ip;
      Num num = counts.get(token);
      if (num == null)
      {
         num = new Num();
         counts.put(token, num);
      }

      num.inc();

      if (num.val() >= hits)
      {
         System.err.println("Rate Limiting Ip: " + token);
         chain.cancel();
         res.setJson(null);
         res.setStatus(SC.SC_429_TOO_MANY_REQUESTS);
      }
   }

   public static String getIp(HttpServletRequest request)
   {
      //      HttpSession session = request.getSession(false);
      //      if(session != null)
      //         return session.getId();

      String ip = request.getHeader("X-Forwarded-For");
      if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
      {
         ip = request.getHeader("Proxy-Client-IP");
      }
      if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
      {
         ip = request.getHeader("WL-Proxy-Client-IP");
      }
      if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
      {
         ip = request.getHeader("HTTP_CLIENT_IP");
      }
      if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
      {
         ip = request.getHeader("HTTP_X_FORWARDED_FOR");
      }
      if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
      {
         ip = request.getRemoteAddr();
      }
      return ip;
   }

}
