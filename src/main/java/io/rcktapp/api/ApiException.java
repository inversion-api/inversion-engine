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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.rcktapp.api;

public class ApiException extends RuntimeException
{
   String status = SC.SC_500_INTERNAL_SERVER_ERROR;

   public static void main(String[] args)
   {
      System.out.println("401 Unauthorized".matches("\\d\\d\\d *"));
   }

   public ApiException(String status)
   {
      super(status);
      if (status.matches("\\d\\d\\d .*"))
         setStatus(status);
   }

   public ApiException(String status, String message)
   {
      super(message);
      setStatus(status);
   }

   public ApiException(String status, String message, Throwable t)
   {
      super(message, t);
      setStatus(status);
   }

   public String getStatus()
   {
      return status;
   }

   public void setStatus(String status)
   {
      this.status = status;
   }

}
