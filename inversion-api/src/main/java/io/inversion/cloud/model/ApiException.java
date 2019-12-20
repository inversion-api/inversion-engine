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
