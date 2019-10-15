/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
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

public class ApiException extends RuntimeException
{
   protected String status = SC.SC_500_INTERNAL_SERVER_ERROR;

   //   public static void main(String[] args)
   //   {
   //      System.out.println("401 Unauthorized".matches("\\d\\d\\d *"));
   //   }

   public ApiException(String status)
   {
      super(status);
      if (status.matches("\\d\\d\\d .*"))
         withStatus(status);
   }

   public ApiException(String status, String message)
   {
      super(message);
      withStatus(status);
   }

   public ApiException(String status, String message, Throwable t)
   {
      super(message, t);
      withStatus(status);
   }

   public String getStatus()
   {
      return status;
   }

   public ApiException withStatus(String status)
   {
      this.status = status;
      return this;
   }

   public boolean hasStatus(int... statusCodes)
   {
      for (int statusCode : statusCodes)
      {
         if (status.startsWith(statusCode + " "))
            return true;
      }
      return false;
   }

}
