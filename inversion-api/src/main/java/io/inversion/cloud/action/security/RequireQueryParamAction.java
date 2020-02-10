/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
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

import java.util.HashSet;
import java.util.Set;

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;

/**
 * Requires query string parameter "requiredName=anyValue" OR "eq(requiredName,anyValue)" be present on the Request.
 * 
 * The param could have been supplied by the call or by another action as long as
 * it is there when this action runs.
 * 
 * @author wells
 */
public class RequireQueryParamAction extends Action<SetQueryParamAction>
{
   protected Set<String> params = new HashSet();

   public RequireQueryParamAction()
   {
      this(null);
   }

   public RequireQueryParamAction(String inludePaths)
   {
      this(inludePaths, null, null);
   }

   public RequireQueryParamAction(String inludePaths, String excludePaths, String config)
   {
      super(inludePaths, excludePaths, config);
   }

   public void run(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      for (String requiredParam : params)
      {
         boolean hasParam = false;
         requiredParam = requiredParam.toLowerCase();
         for (String param : req.getParams().keySet())
         {
            param = param.toLowerCase();

            if (requiredParam.equals(param))
               hasParam = true;
            else if (param.startsWith("eq(" + requiredParam + ","))
               hasParam = true;
         }
         if (!hasParam)
            throw new ApiException(SC.SC_400_BAD_REQUEST, "Required query string parameter '" + requiredParam + "' appears to be missing.");
      }
   }

   public RequireQueryParamAction withParam(String name, String value)
   {
      params.add(name);
      return this;
   }
}
