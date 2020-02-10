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
package io.inversion.cloud.action.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.Utils;

/**
 * The AclAction secures an API by making sure that a requests matches one or 
 * more declared AclRules.apple.com
 * 
 * AclRules specify the roles and permissions that a user must have to access
 * specific method/path combinations and can also specify input/output
 * parameters that are either required or restricted
 * 
 * @author wells
 *
 */
public class AclAction extends Action<AclAction>
{
   Logger                  log      = LoggerFactory.getLogger(AclAction.class);

   protected List<AclRule> aclRules = new ArrayList();

   public AclAction()
   {
      this(null);
   }

   public AclAction(String includePaths, AclRule... aclRules)
   {
      this(null, includePaths, null, null, aclRules);
   }

   public AclAction(String methods, String includePaths, String excludePaths, String config, AclRule... aclRules)
   {
      super(includePaths, excludePaths, config);

      withOrder(500);
      withMethods(methods);

      if (aclRules != null)
         withAclRules(aclRules);
   }

   public AclAction withAclRule(String name, String methods, String includePaths, String... permissions)
   {
      return withAclRules(new AclRule(name, methods, includePaths, permissions));
   }

   public AclAction withAclRules(AclRule... acls)
   {
      for (AclRule acl : acls)
      {
         if (!aclRules.contains(acl))
         {
            aclRules.add(acl);
         }
      }

      Collections.sort(aclRules);
      return this;
   }

   @Override
   public void run(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response resp) throws Exception
   {
      List<AclRule> matched = new ArrayList<>();
      boolean allowed = false;

      log.debug("Request Path: " + req.getPath());

      for (AclRule aclRule : aclRules)
      {
         if (aclRule.ruleMatches(req))
         {
            //log.debug("Matched AclAction: " + aclRule.getName());
            if (!aclRule.isAllow())
            {
               Chain.debug("AclAction: MATCH_DENY" + aclRule);

               allowed = false;
               break;
            }
            else
            {
               if (!aclRule.isInfo() && aclRule.isAllow())
               {
                  Chain.debug("AclAction: MATCH_ALLOW " + aclRule);
                  allowed = true;
               }
               else
               {
                  Chain.debug("AclAction: MATCH_INFO " + aclRule);
               }
            }

            matched.add(aclRule);
         }
      }

      if (!allowed)
      {
         Chain.debug("AclAction: NO_MATCH_DENY");
         throw new ApiException(SC.SC_403_FORBIDDEN);
      }

      Set requires = new HashSet();
      Set restricts = new HashSet();

      for (AclRule aclRule : matched)
      {
         requires.addAll(aclRule.getRequires());
         restricts.addAll(aclRule.getRestricts());
      }

      if (!requires.isEmpty())
         Chain.debug("AclAction: requires: " + requires);

      if (!restricts.isEmpty())
         Chain.debug("AclAction: restricts: " + restricts);

      cleanParams(chain, req, restricts, requires);
      cleanJson(chain, req.getJson(), restricts, requires, false);

      try
      {
         chain.go();
      }
      finally
      {
         JSNode json = resp.getJson();
         if (json != null)
         {
            List toClean = json instanceof JSArray ? ((JSArray) json).asList() : Arrays.asList(json);
            for (Object parent : toClean)
            {
               if (parent instanceof JSNode)
               {
                  cleanJson(chain, (JSNode) parent, restricts, Collections.EMPTY_SET, true);
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
               req.withParam(key1, value);
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
                     req.withParam(key, value);
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
               req.withParam(required, value);
               found = true;
            }
         }

         if (!found)
         {
            throw new ApiException(SC.SC_400_BAD_REQUEST, "Missing required param '" + required + "'");
         }
      }
   }

   void cleanJson(Chain chain, JSNode json, Set<String> restricts, Set<String> requires, boolean silent)
   {
      if (json != null)
      {
         List objs = json instanceof JSArray ? ((JSArray) json).asList() : Arrays.asList(json);

         for (String path : restricts)
         {
            List<JSNode> found = new ArrayList();

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

            for (JSNode target : found)
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
            List<JSNode> found = new ArrayList();

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

            for (JSNode target : found)
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
