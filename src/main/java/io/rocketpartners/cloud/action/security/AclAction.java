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
package io.rocketpartners.cloud.action.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.rocketpartners.cloud.model.AclRule;
import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Request;
import io.rocketpartners.cloud.service.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.JSArray;
import io.rocketpartners.cloud.utils.JSObject;
import io.rocketpartners.cloud.utils.Utils;

public class AclAction extends Action<AclAction>
{
   Logger log = LoggerFactory.getLogger(AclAction.class);

   @Override
   public void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response resp) throws Exception
   {
      List<AclRule> matched = new ArrayList<>();
      boolean allowed = false;

      log.debug("Request Path: " + req.getPath());

      for (AclRule aclRule : api.getAclRules())
      {
         if (aclRule.ruleMatches(req))
         {
            log.debug("Matched ACL: " + aclRule.getName());
            if (!aclRule.isAllow())
            {
               allowed = false;
               break;
            }
            else if (!aclRule.isInfo() && aclRule.isAllow())
            {
               allowed = true;
            }

            matched.add(aclRule);
         }
      }

      if (!allowed)
      {
         throw new ApiException(SC.SC_403_FORBIDDEN);
      }

      Set requires = new HashSet();
      Set restricts = new HashSet();

      for (AclRule aclRule : matched)
      {
         requires.addAll(aclRule.getRequires());
         restricts.addAll(aclRule.getRestricts());
      }

      log.debug("ACL requires: " + requires);
      log.debug("ACL restricts: " + restricts);

      cleanParams(chain, req, restricts, requires);
      cleanJson(chain, req.getJson(), restricts, requires, false);

      try
      {
         chain.go();
      }
      finally
      {
         JSObject json = resp.getJson();
         if (json != null)
         {
            List toClean = json instanceof JSArray ? ((JSArray) json).asList() : Arrays.asList(json);
            for (Object parent : toClean)
            {
               if (parent instanceof JSObject)
               {
                  cleanJson(chain, (JSObject) parent, restricts, Collections.EMPTY_SET, true);
               }
            }
         }
      }
   }

   void cleanParams(Chain chain, Request req, Set<String> restricts, Set<String> requires)
   {
      for (String restricted : restricts)
      {
         restricted = restricted.toLowerCase();

         if (restricted.indexOf("=") > 0)
         {
            String key1 = restricted.split("=")[0];
            String value = restricted.split("=")[1].trim();

            if (value.startsWith("${"))
               value = getValue(chain, value.substring(2, value.length() - 1));

            if ("entitykey".equals(key1))
            {
               req.withEntityKey(value);
            }
            else
            {
               req.putParam(key1, value);
            }
            continue;
         }

         if (restricted.startsWith("query.") || restricted.startsWith("*."))
            restricted = restricted.substring(restricted.indexOf(".") + 1, restricted.length());

         if (restricted.indexOf(".") > 0)
            continue;

         for (String key : req.getParams().keySet())
         {
            String value = req.getParam(key);
            if (matchesVal(restricted, key) || matchesVal(restricted, value))
            {
               throw new ApiException(SC.SC_400_BAD_REQUEST, "Unknown or invalid query param '" + key + "=" + value + "'.");
            }
         }
      }

      for (String required : requires)
      {
         required = required.toLowerCase();

         if (required.startsWith("query.") || required.startsWith("*."))
            required = required.substring(required.indexOf(".") + 1, required.length());

         if (required.indexOf(".") > 0)
            continue;

         boolean found = false;
         for (String key : req.getParams().keySet())
         {
            if (matchesVal(required, key))
            {
               String value = req.getParam(key);
               if (Utils.empty(value))
               {
                  value = getValue(chain, key);
                  if (value != null)
                     req.putParam(key, value);
               }

               if (value != null)
               {
                  found = true;
                  break;
               }
            }
         }

         if (!found)
         {
            String value = getValue(chain, required);
            if (value != null)
            {
               req.putParam(required, value);
               found = true;
            }
         }

         if (!found)
         {
            throw new ApiException(SC.SC_400_BAD_REQUEST, "Missing required param '" + required + "'");
         }
      }
   }

   void cleanJson(Chain chain, JSObject json, Set<String> restricts, Set<String> requires, boolean silent)
   {
      if (json != null)
      {
         List objs = json instanceof JSArray ? ((JSArray) json).asList() : Arrays.asList(json);

         for (String path : restricts)
         {
            List<JSObject> found = new ArrayList();

            String parentPath = (path.lastIndexOf(".") < 0 ? "" : path.substring(0, path.lastIndexOf("."))).toLowerCase();
            String targetProp = path.lastIndexOf(".") < 0 ? path : path.substring(path.lastIndexOf(".") + 1, path.length());

            if (parentPath.startsWith("query."))
               continue;
            if (parentPath.startsWith("*."))
               parentPath = "*." + parentPath;
            if (!parentPath.startsWith("body."))
               parentPath = "body." + parentPath;

            for (Object parent : objs)
            {
               find(parent, found, parentPath, "body.");
            }

            for (JSObject target : found)
            {
               target.remove(targetProp);
               if (!silent)
               {
                  if (target.containsKey(targetProp))
                     throw new ApiException(SC.SC_400_BAD_REQUEST, "Unknown or invalid JSON property '" + path + "'.");
               }
            }
         }

         for (String path : requires)
         {
            List<JSObject> found = new ArrayList();

            String parentPath = (path.lastIndexOf(".") < 0 ? "" : path.substring(0, path.lastIndexOf("."))).toLowerCase();
            String targetProp = path.lastIndexOf(".") < 0 ? path : path.substring(path.lastIndexOf(".") + 1, path.length());

            if (parentPath.startsWith("query."))
               continue;
            if (parentPath.startsWith("*."))
               parentPath = "*." + parentPath;
            if (!parentPath.startsWith("body."))
               parentPath = "body." + parentPath;

            for (Object parent : objs)
            {
               find(parent, found, parentPath, "body.");
            }

            for (JSObject target : found)
            {
               if (target.keySet().size() == 1 && target.containsKey("href"))
               {
                  //this is posting some type of reference not a json body, this 
                  //should not result in the entity being written
                  continue;
               }

               Object value = target.get(targetProp);
               if (value == null)
               {
                  value = getValue(chain, targetProp);

                  if (value != null)
                     target.put(targetProp, value);
                  else
                     throw new ApiException(SC.SC_400_BAD_REQUEST, "Required property '" + path + "' is missing from JSON body");

               }
            }
         }
      }
   }

   boolean matchesVal(String restricted, String value)
   {
      if (restricted == null || value == null)
         return false;

      return value.toLowerCase().matches(".*\\b" + restricted.toLowerCase() + "\\b.*");
   }
}
