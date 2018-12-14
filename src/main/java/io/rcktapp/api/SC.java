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
package io.rcktapp.api;

/**
 * @see http://www.restapitutorial.com/httpstatuscodes.html
 */
public class SC
{

   public static final String SC_200_OK                    = "200 OK";
   public static final String SC_201_CREATED               = "201 Created";
   public static final String SC_400_BAD_REQUEST           = "400 Bad Request";
   public static final String SC_401_UNAUTHORIZED          = "401 Unauthorized";
   public static final String SC_403_FORBIDDEN             = "403 Forbidden";
   public static final String SC_429_TOO_MANY_REQUESTS     = "429 Too Many Requests";
   public static final String SC_404_NOT_FOUND             = "404 Not Found";
   public static final String SC_500_INTERNAL_SERVER_ERROR = "500 Internal Server Error";

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
