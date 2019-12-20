/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
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
package io.inversion.cloud.model;

import java.util.HashMap;
import java.util.Map;

/**
 * @see http://www.restapitutorial.com/httpstatuscodes.html
 */
public class SC
{

   public static final String         SC_200_OK                    = "200 OK";
   public static final String         SC_201_CREATED               = "201 Created";
   public static final String         SC_204_NO_CONTENT            = "204 No Content";

   //@see https://developer.mozilla.org/en-US/docs/Web/HTTP/Redirections
   //public static final String         SC_302_FOUND                 = "302 Found";
   public static final String         SC_308_PERMANENT_REDIRECT    = "308 Permanent Redirect";

   public static final String         SC_400_BAD_REQUEST           = "400 Bad Request";
   public static final String         SC_401_UNAUTHORIZED          = "401 Unauthorized";
   public static final String         SC_403_FORBIDDEN             = "403 Forbidden";
   public static final String         SC_404_NOT_FOUND             = "404 Not Found";
   public static final String         SC_429_TOO_MANY_REQUESTS     = "429 Too Many Requests";
   public static final String         SC_500_INTERNAL_SERVER_ERROR = "500 Internal Server Error";
   public static final String         SC_501_NOT_IMPLEMENTED       = "501 Not Implemented";

   public static Map<Integer, String> SC_MAP                       = new HashMap<>();
   static
   {
      SC_MAP.put(200, SC_200_OK);
      SC_MAP.put(201, SC_201_CREATED);
      //SC_MAP.put(302, SC_302_FOUND);
      SC_MAP.put(302, SC_308_PERMANENT_REDIRECT);
      SC_MAP.put(400, SC_400_BAD_REQUEST);
      SC_MAP.put(401, SC_401_UNAUTHORIZED);
      SC_MAP.put(403, SC_403_FORBIDDEN);
      SC_MAP.put(404, SC_404_NOT_FOUND);
      SC_MAP.put(429, SC_429_TOO_MANY_REQUESTS);
      SC_MAP.put(500, SC_500_INTERNAL_SERVER_ERROR);
      SC_MAP.put(501, SC_501_NOT_IMPLEMENTED);
   }

   public static boolean matches(Object responseCode, int... choices)
   {
      String code = responseCode.toString();
      if (code.indexOf(" ") > 0)
         code = code.substring(0, code.indexOf(" "));

      int num = Integer.parseInt(code);

      for (int i = 0; i < choices.length; i++)
      {
         if (num == choices[i])
            return true;
      }

      return false;
   }

}
