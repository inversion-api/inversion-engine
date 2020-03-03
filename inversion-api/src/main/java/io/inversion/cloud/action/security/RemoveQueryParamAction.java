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
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class RemoveQueryParamAction extends Action<SetQueryParamAction>
{
   protected Set<String> params = new HashSet();

   public void run(Request req, Response res) throws Exception
   {
      for (String removedParam : params)
      {
         for (String param : req.getParams().keySet())
         {
            if (containsParam(removedParam, param))
            {
               req.removeParam(param);
            }
         }
      }
   }

   public RemoveQueryParamAction withParam(String name, String value)
   {
      params.add(name);
      return this;
   }

   /**
    * Checks for a whole word case insensitive match of <code>findThisParamName</code>
    * in <code>inThisString</code>
    * 
    * https://www.baeldung.com/java-regexp-escape-char
    * https://stackoverflow.com/questions/7459263/regex-whole-word
    * 
    * @param findThisParamName
    * @param inThisString
    */
   public static boolean containsParam(String findThisParamName, String inThisString)
   {
      String regex = "\\b\\Q" + findThisParamName + "\\E\\b";
      return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(inThisString).find();
   }

}
