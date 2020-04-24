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
package io.inversion.cloud.model;

import io.inversion.cloud.utils.Utils;

import java.io.StringWriter;
import java.util.Formatter;
import java.util.Locale;

public class ApiException extends RuntimeException implements Status
{
   protected String status = Status.SC_500_INTERNAL_SERVER_ERROR;

   public ApiException() throws ApiException
   {
      this(SC_500_INTERNAL_SERVER_ERROR, null, null);
   }

   public ApiException(Throwable cause) throws ApiException
   {
      this(SC_500_INTERNAL_SERVER_ERROR, cause, null);
   }

   public ApiException(String httpStatus) throws ApiException
   {
      this(httpStatus, null, null);
   }

   public ApiException(String messageFormat, Object... args) throws ApiException
   {
      this(SC_500_INTERNAL_SERVER_ERROR, null, messageFormat, args);
   }

   public ApiException(String httpStatus, Throwable cause, String messageFormat, Object... args) throws ApiException
   {
      super(getMessage(httpStatus, cause, messageFormat, args), cause != null ? Utils.getCause(cause) : null);

      if (httpStatus == null && messageFormat != null)
         httpStatus = messageFormat;

      if (httpStatus.matches("\\d\\d\\d .*"))
         withStatus(httpStatus);
   }

   /**
    * Supports error message construction including variable replacement supporting
    * logging framework "{}" placesholders OR java.util.Formatter style placeholders.
    * <p> Any "{}" substrings in <code>messageFormat</code> are replaced with "{}"
    * before <code>messageFormat</code> and <code>args</code> are passed to a 
    * java.util.Formatter.
    * <p>
    * All arguments are optional but if everything is null you will get an empty string.
    *
    *  
    * @param httpStatus 
    * @param cause
    * @param messageFormat
    * @param args
    * @return
    */
   public static String getMessage(String httpStatus, Throwable cause, String messageFormat, Object... args)
   {
      String msg = httpStatus != null ? httpStatus : "";
      if (messageFormat != null)
      {
         if (args != null && args.length > 0)
         {
            //-- most logging frameworks are using "{}" to indicate
            //-- var placeholders these days
            messageFormat = messageFormat.replace("{}", "{}");

            StringWriter sw = new StringWriter();
            Formatter fmt = new Formatter(sw);
            fmt.format(Locale.getDefault(), messageFormat, args);
            fmt.close();
            messageFormat = sw.toString();
         }
         msg += msg.length() > 0 ? " " + messageFormat : messageFormat;
      }

      if (cause != null)
      {
         String causeStr = Utils.getShortCause(cause);
         msg = msg.length() > 0 ? (msg + "\r\n") + causeStr : causeStr;
      }

      return msg;
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

   public static void throw400BadRequest() throws ApiException
   {
      throwEx(SC_400_BAD_REQUEST, null, null);
   }

   public static void throw400BadRequest(Throwable cause) throws ApiException
   {
      throwEx(SC_400_BAD_REQUEST, cause, null);
   }

   public static void throw400BadRequest(String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_400_BAD_REQUEST, null, messageFormat, messages);
   }

   public static void throw400BadRequest(Throwable cause, String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_400_BAD_REQUEST, cause, messageFormat, messages);
   }

   public static void throw401Unauthroized() throws ApiException
   {
      throwEx(SC_401_UNAUTHORIZED, null, null);
   }

   public static void throw401Unauthroized(Throwable cause) throws ApiException
   {
      throwEx(SC_401_UNAUTHORIZED, cause, null);
   }

   public static void throw401Unauthroized(String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_401_UNAUTHORIZED, null, messageFormat, messages);
   }

   public static void throw401Unauthroized(Throwable cause, String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_401_UNAUTHORIZED, cause, messageFormat, messages);
   }

   public static void throw403Forbidden() throws ApiException
   {
      throwEx(SC_403_FORBIDDEN, null, null);
   }

   public static void throw403Forbidden(Throwable cause) throws ApiException
   {
      throwEx(SC_403_FORBIDDEN, cause, null);
   }

   public static void throw403Forbidden(String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_403_FORBIDDEN, null, messageFormat, messages);
   }

   public static void throw403Forbidden(Throwable cause, String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_403_FORBIDDEN, cause, messageFormat, messages);
   }

   public static void throw404NotFound() throws ApiException
   {
      throwEx(SC_404_NOT_FOUND, null, null);
   }

   public static void throw404NotFound(Throwable cause) throws ApiException
   {
      throwEx(SC_404_NOT_FOUND, cause, null);
   }

   public static void throw404NotFound(String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_404_NOT_FOUND, null, messageFormat, messages);
   }

   public static void throw404NotFound(Throwable cause, String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_404_NOT_FOUND, cause, messageFormat, messages);
   }

   public static void throw429TooManyRequests() throws ApiException
   {
      throwEx(SC_429_TOO_MANY_REQUESTS, null, null);
   }

   public static void throw429TooManyRequests(Throwable cause) throws ApiException
   {
      throwEx(SC_429_TOO_MANY_REQUESTS, cause, null);
   }

   public static void throw429TooManyRequests(String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_429_TOO_MANY_REQUESTS, null, messageFormat, messages);
   }

   public static void throw429TooManyRequests(Throwable cause, String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_429_TOO_MANY_REQUESTS, cause, messageFormat, messages);
   }

   public static void throw500InternalServerError() throws ApiException
   {
      throwEx(SC_500_INTERNAL_SERVER_ERROR, null, null);
   }

   public static void throw500InternalServerError(Throwable cause) throws ApiException
   {
      throwEx(SC_500_INTERNAL_SERVER_ERROR, cause, null);
   }

   public static void throw500InternalServerError(String messageFormat, Object... args) throws ApiException
   {
      throwEx(SC_500_INTERNAL_SERVER_ERROR, null, messageFormat, args);
   }

   public static void throw500InternalServerError(Throwable cause, String messageFormat, Object... args) throws ApiException
   {
      throwEx(SC_500_INTERNAL_SERVER_ERROR, cause, messageFormat, args);
   }

   public static void throw501NotImplemented() throws ApiException
   {
      throwEx(SC_501_NOT_IMPLEMENTED, null, null);
   }

   public static void throw501NotImplemented(Throwable cause) throws ApiException
   {
      throwEx(SC_501_NOT_IMPLEMENTED, cause, null);
   }

   public static void throw501NotImplemented(String messageFormat, Object... args) throws ApiException
   {
      throwEx(SC_501_NOT_IMPLEMENTED, null, messageFormat, args);
   }

   public static void throw501NotImplemented(Throwable cause, String messageFormat, Object... args) throws ApiException
   {
      throwEx(SC_501_NOT_IMPLEMENTED, cause, messageFormat, args);
   }

   /**
    * Rethrows <code>cause</code> as a 500  INTERNAL SERVER ERROR ApiException
    * 
    * @param cause
    * @throws ApiException
    */
   public static void throwEx(Throwable cause) throws ApiException
   {
      throwEx(SC_500_INTERNAL_SERVER_ERROR, cause, null);
   }

   /**
    * Throws a 500 INTERNAL SERVER ERROR ApiException with the given message
    * 
    * @param messageFormat
    * @param messages
    * @throws ApiException
    */
   public static void throwEx(String messageFormat, Object... messages) throws ApiException
   {
      throwEx(SC_500_INTERNAL_SERVER_ERROR, null, messageFormat, messages);
   }

   public static void throwEx(String status, Throwable cause, String messageFormat, Object... args) throws ApiException
   {
      ApiException ex = new ApiException(status, cause, messageFormat, args);
      throw ex;
   }

}
