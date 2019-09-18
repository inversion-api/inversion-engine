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

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.ArrayNode;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Utils;

/**
 * The AclAction secures an API by making sure that a requests matches one or 
 * more declared AclRules.
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
   public void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response resp) throws Exception
   {
      List<AclRule> matched = new ArrayList<>();
      boolean allowed = false;

      log.debug("Request Path: " + req.getPath());

      for (AclRule aclRule : aclRules)
      {
         if (aclRule.ruleMatches(req))
         {
            //log.debug("Matched ACL: " + aclRule.getName());
            if (!aclRule.isAllow())
            {
               Chain.debug("ACL: MATCH_DENY" + aclRule);

               allowed = false;
               break;
            }
            else
            {
               if (!aclRule.isInfo() && aclRule.isAllow())
               {
                  Chain.debug("ACL: MATCH_ALLOW " + aclRule);
                  allowed = true;
               }
               else
               {
                  Chain.debug("ACL: MATCH_INFO " + aclRule);
               }
            }

            matched.add(aclRule);
         }
      }

      if (!allowed)
      {
         Chain.debug("ACL: NO_MATCH_DENY");
         throw new ApiException(SC.SC_403_FORBIDDEN);
      }

      Set requires = new HashSet();
      Set restricts = new HashSet();

      for (AclRule aclRule : matched)
      {
         requires.addAll(aclRule.getRequires());
         restricts.addAll(aclRule.getRestricts());
      }

      Chain.debug("ACL requires: " + requires);
      Chain.debug("ACL restricts: " + restricts);

      cleanParams(chain, req, restricts, requires);
      cleanJson(chain, req.getJson(), restricts, requires, false);

      try
      {
         chain.go();
      }
      finally
      {
         ObjectNode json = resp.getJson();
         if (json != null)
         {
            List toClean = json instanceof ArrayNode ? ((ArrayNode) json).asList() : Arrays.asList(json);
            for (Object parent : toClean)
            {
               if (parent instanceof ObjectNode)
               {
                  cleanJson(chain, (ObjectNode) parent, restricts, Collections.EMPTY_SET, true);
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

   void cleanJson(Chain chain, ObjectNode json, Set<String> restricts, Set<String> requires, boolean silent)
   {
      if (json != null)
      {
         List objs = json instanceof ArrayNode ? ((ArrayNode) json).asList() : Arrays.asList(json);

         for (String path : restricts)
         {
            List<ObjectNode> found = new ArrayList();

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

            for (ObjectNode target : found)
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
            List<ObjectNode> found = new ArrayList();

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

            for (ObjectNode target : found)
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
