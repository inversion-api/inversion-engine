/*
 * Copyright (c) 2015-2018 Inversion.org, LLC
 * https://github.com/inversion-api
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
   public static final String         SC_302_FOUND                 = "302 Found";
   public static final String         SC_400_BAD_REQUEST           = "400 Bad Request";
   public static final String         SC_401_UNAUTHORIZED          = "401 Unauthorized";
   public static final String         SC_403_FORBIDDEN             = "403 Forbidden";
   public static final String         SC_404_NOT_FOUND             = "404 Not Found";
   public static final String         SC_429_TOO_MANY_REQUESTS     = "429 Too Many Requests";
   public static final String         SC_500_INTERNAL_SERVER_ERROR = "500 Internal Server Error";

   public static Map<Integer, String> SC_MAP                       = new HashMap<>();
   static
   {
      SC_MAP.put(200, SC_200_OK);
      SC_MAP.put(201, SC_201_CREATED);
      SC_MAP.put(302, SC_302_FOUND);
      SC_MAP.put(400, SC_400_BAD_REQUEST);
      SC_MAP.put(401, SC_401_UNAUTHORIZED);
      SC_MAP.put(403, SC_403_FORBIDDEN);
      SC_MAP.put(404, SC_404_NOT_FOUND);
      SC_MAP.put(429, SC_429_TOO_MANY_REQUESTS);
      SC_MAP.put(500, SC_500_INTERNAL_SERVER_ERROR);
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
