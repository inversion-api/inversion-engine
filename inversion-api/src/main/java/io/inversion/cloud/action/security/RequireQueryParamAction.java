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

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.rql.RqlParser;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.utils.Utils;

import java.util.HashSet;
import java.util.Set;

/**
 * Requires query string parameter "requiredParam=anyValue" OR "eq(requiredParam,anyValue)" be present on the request url querystring 
 * with a non empty or 'null' value.
 * 
 * The param could have been supplied by the caller or by another action as long as it is there when this action runs.
 * 
 * @author wells
 */
public class RequireQueryParamAction extends Action<SetQueryParamAction>
{
   protected Set<String> params = new HashSet();

   public void run(Request req, Response res) throws Exception
   {
      for (String requiredParam : params)
      {
         boolean hasParam = false;
         requiredParam = requiredParam.toLowerCase();
         for (String param : req.getUrl().getParams().keySet())
         {
            if (Utils.containsToken(requiredParam, param))
            {
               String value = req.getUrl().getParam(param);
               if (requiredParam.equalsIgnoreCase(param.trim()))
               {
                  if (!Utils.empty(value))
                  {
                     hasParam = true;
                     break;
                  }
               }
               else
               {
                  //-- eq(param,value) format
                  Term t = new RqlParser().parse(param.toLowerCase());
                  if (requiredParam.equalsIgnoreCase(t.getToken(0)) && t.hasToken("eq") && t.size() == 2 && t.getTerm(1).isLeaf() && !"null".equalsIgnoreCase(t.getToken(1)))
                  {
                     hasParam = true;
                     break;
                  }
               }
            }
         }
         if (!hasParam)
            ApiException.throw400BadRequest("Required query string parameter '{}' appears to be missing.", requiredParam);
      }
   }

   public RequireQueryParamAction withParam(String name, String value)
   {
      params.add(name);
      return this;
   }
}
