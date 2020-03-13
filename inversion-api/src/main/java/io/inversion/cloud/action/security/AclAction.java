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
package io.inversion.cloud.action.security;

import io.inversion.cloud.model.*;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.utils.Utils;

import java.util.*;

/**
 * The AclAction secures an API by making sure that a requests matches one or 
 * more declared AclRules
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
   protected List<AclRule> aclRules = new ArrayList();

   public AclAction orRequireAllPerms(String httpMethods, String includePaths, String permission1, String... permissionsN)
   {
      withAclRules(AclRule.requireAllPerms(httpMethods, includePaths, permission1, permissionsN));
      return this;
   }

   public AclAction orRequireOnePerm(String httpMethods, String includePaths, String permission1, String... permissionsN)
   {
      withAclRules(AclRule.requireOnePerm(httpMethods, includePaths, permission1, permissionsN));
      return this;
   }

   public AclAction orRequireAllRoles(String httpMethods, String includePaths, String role1, String... rolesN)
   {
      withAclRules(AclRule.requireAllRoles(httpMethods, includePaths, role1, rolesN));
      return this;
   }

   public AclAction orRequireOneRole(String httpMethods, String includePaths, String role1, String... rolesN)
   {
      withAclRules(AclRule.requireOneRole(httpMethods, includePaths, role1, rolesN));
      return this;
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
   public void run(Request req, Response resp) throws Exception
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
         ApiException.throw403Forbidden();
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

      cleanParams(req, restricts, requires);
      cleanJson(req, req.getJson(), restricts, requires, false);

      try
      {
         req.getChain().go();
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
                  cleanJson(req, (JSNode) parent, restricts, Collections.EMPTY_SET, true);
               }
            }
         }
      }
   }

   void cleanParams(Request req, Set<String> restricts, Set<String> requires)
   {
      for (String restricted : restricts)
      {
         restricted = restricted.toLowerCase();

         if (restricted.indexOf("=") > 0)
         {
            String key1 = restricted.split("=")[0];
            String value = restricted.split("=")[1].trim();

            if (value.startsWith("${"))
               value = getValue(req.getChain(), value.substring(2, value.length() - 1));

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
               ApiException.throw500InternalServerError("Unknown or invalid query param '%s'='%s'.", key, value);
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
                  value = getValue(req.getChain(), key);
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
            String value = getValue(req.getChain(), required);
            if (value != null)
            {
               req.withParam(required, value);
               found = true;
            }
         }

         if (!found)
         {
            ApiException.throw400BadRequest("Missing required param '%s'", required);
         }
      }
   }

   void cleanJson(Request req, JSNode json, Set<String> restricts, Set<String> requires, boolean silent)
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
                     ApiException.throw400BadRequest("Unknown or invalid JSON property '%s'.", path);
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
                  value = getValue(req.getChain(), targetProp);

                  if (value != null)
                     target.put(targetProp, value);
                  else
                     ApiException.throw400BadRequest("Required property '%s' is missing from JSON body", path);

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

   public static String getValue(Chain chain, String key)
   {
      key = key.toLowerCase().trim();

      if ("api".equals(key))
      {
         return chain.getRequest().getApi().getName();
      }
      else if (Utils.in(key, "tenant", "tenantid", "tenantcode"))
      {
         if (Chain.getUser() != null)
            return Chain.getUser().getTenant();
      }
      else if ("user".equals(key))
      {
         if (Chain.getUser() != null)
            return Chain.getUser().getId() + "";
      }
      else if ("username".equals(key))
      {
         if (Chain.getUser() != null)
            return Chain.getUser().getUsername();
      }

      Object val = chain.get(key);
      if (val != null)
         return val.toString();
      return null;
   }

   public static void find(Object parent, List<JSNode> found, String targetPath, String currentPath)
   {
      if (parent instanceof JSArray)
      {
         for (Object child : (JSArray) parent)
         {
            if (child instanceof JSNode)
               find(child, found, targetPath, currentPath);
         }
      }
      else if (parent instanceof JSNode)
      {
         if (!found.contains(parent) && Utils.wildcardMatch(targetPath, currentPath))
         {
            found.add((JSNode) parent);
         }

         for (String key : ((JSNode) parent).keySet())
         {
            Object child = ((JSNode) parent).get(key);
            String nextPath = currentPath == null || currentPath.length() == 0 ? key : currentPath + key.toLowerCase() + ".";
            find(child, found, targetPath, nextPath);
         }
      }
   }

   public static List<JSNode> find(Object parent, String... paths)
   {
      List<JSNode> found = new ArrayList();
      for (String apath : paths)
      {
         for (String path : (List<String>) Utils.explode(",", apath))
         {
            find(parent, found, path, ".");
         }
      }
      return found;
   }

}
