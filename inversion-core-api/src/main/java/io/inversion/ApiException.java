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
package io.inversion;

import io.inversion.utils.Utils;

public class ApiException extends RuntimeException implements Status {
    protected String status = Status.SC_500_INTERNAL_SERVER_ERROR;

    public ApiException() throws ApiException {
        this((Exception)null, SC_500_INTERNAL_SERVER_ERROR, null);
    }

    public ApiException(Throwable cause) throws ApiException {
        this(cause, SC_500_INTERNAL_SERVER_ERROR, null);
    }

//    public ApiException(String httpStatus) throws ApiException {
//        this(httpStatus, null, null);
//    }

//    public ApiException(String httpStatus, String messageFormat, Object... args) throws ApiException {
//        this(null, httpStatus, messageFormat, args);
//    }

    public ApiException(String messageFormat, Object... args) throws ApiException {
        this((Exception)null, SC_500_INTERNAL_SERVER_ERROR, messageFormat, args);
    }

    public ApiException(Throwable cause, String httpStatus, String messageFormat, Object... args) throws ApiException {
        super(getMessage(cause, httpStatus, messageFormat, args), cause != null ? Utils.getCause(cause) : null);

        //-- someone could have used the wrong constructor and the message format could be the httpStatus
        if (httpStatus == null && messageFormat != null && messageFormat.matches("\\d\\d\\d .*"))
            httpStatus = messageFormat;

        if(httpStatus == null)
            httpStatus = Status.SC_500_INTERNAL_SERVER_ERROR;

        //-- someone passed in a junk httpStatus string
        if(!httpStatus.matches("\\d\\d\\d .*"))
            httpStatus = Status.SC_500_INTERNAL_SERVER_ERROR + " - " + httpStatus;

        withStatus(httpStatus);
    }


    public String getStatus() {
        return status;
    }

    public ApiException withStatus(String status) {
        this.status = status;
        return this;
    }

    public boolean hasStatus(int... statusCodes) {
        for (int statusCode : statusCodes) {
            if (status.startsWith(statusCode + " "))
                return true;
        }
        return false;
    }

    public int getStatusCode(){
        return Integer.parseInt(status.substring(0, 3));
    }

    /**
     * Constructs a useful error message.
     * <p>
     * All arguments are optional but if everything is null you will get an empty string.
     *
     * @param cause         the cause of the error
     * @param httpStatus    the HTTP stats codde of the error
     * @param messageFormat the caller supplied error message with variable placeholders
     * @param args          variables to insert into <code>messageFormat</code>
     * @return a hopefully user friendly error message
     * 
     * @see Utils#format(String, Object...)
     */
    public static String getMessage(Throwable cause, String httpStatus, String messageFormat, Object... args) {

        //-- someone could have used the wrong constructor and the message format could be the httpStatus
        if (httpStatus == null && messageFormat != null && messageFormat.matches("\\d\\d\\d .*"))
            httpStatus = messageFormat;

        if(httpStatus == null)
            httpStatus = Status.SC_500_INTERNAL_SERVER_ERROR;

        //-- someone passed in a junk httpStatus string
        if(!httpStatus.matches("\\d\\d\\d .*"))
            httpStatus = Status.SC_500_INTERNAL_SERVER_ERROR + " - " + httpStatus;

        String msg = httpStatus;

        if(messageFormat != null || (args != null  && args.length > 0))
            msg += " - " + Utils.format(messageFormat, args);

        if (cause != null) {
            String causeStr = Utils.getShortCause(cause);
            msg = msg.length() > 0 ? (msg + "\r\n") + causeStr : causeStr;
        }

        return msg;
    }

    /**
     * Rethrows <code>cause</code> as a "500 Internal Server Error" ApiException
     *
     * @param cause the cause of the error
     * @throws ApiException always
     */
    public static void throwEx(Throwable cause) throws ApiException {
        throw new ApiException(cause);
    }

    /**
     * Throws a "500 Internal Server Error" ApiException with the given message
     *
     * @param messageFormat the error message potentially with variables
     * @param args          values substituted into <code>messageFormat</code>
     * @throws ApiException always
     */
    public static void throwEx(String messageFormat, Object... args) throws ApiException {
        throwEx(null, SC_500_INTERNAL_SERVER_ERROR, messageFormat, args);
    }

    public static void throwEx(Throwable cause, String status, String messageFormat, Object... args) throws ApiException {
        throw new ApiException(cause, status, messageFormat, args);
    }

    public static ApiException new400BadRequest() throws ApiException {
        return new ApiException((Throwable)null, SC_400_BAD_REQUEST, null);
    }

    public static ApiException new400BadRequest(Throwable cause) throws ApiException {
        return new ApiException(cause, SC_400_BAD_REQUEST, null);
    }

    public static ApiException new400BadRequest(String messageFormat, Object... args) throws ApiException {
        return new ApiException(null, SC_400_BAD_REQUEST, messageFormat, args);
    }


    public static ApiException new400BadRequest(Throwable cause, String messageFormat, Object... args) throws ApiException {
        return new ApiException(cause, SC_400_BAD_REQUEST, messageFormat, args);
    }

    public static ApiException new401Unauthroized() throws ApiException {
        return new ApiException((Throwable)null, SC_401_UNAUTHORIZED, null);
    }

    public static ApiException new401Unauthroized(Throwable cause) throws ApiException {
        return new ApiException(cause, SC_401_UNAUTHORIZED, null);
    }

    public static ApiException new401Unauthroized(String messageFormat, Object... args) throws ApiException {
        return new ApiException(null, SC_401_UNAUTHORIZED, messageFormat, args);
    }

    public static ApiException new401Unauthroized(Throwable cause, String messageFormat, Object... args) throws ApiException {
        return new ApiException(cause, SC_401_UNAUTHORIZED, messageFormat, args);
    }

    public static ApiException new403Forbidden() throws ApiException {
        return new ApiException((Throwable)null, SC_403_FORBIDDEN, null);
    }

    public static ApiException new403Forbidden(Throwable cause) throws ApiException {
        return new ApiException(cause, SC_403_FORBIDDEN, null);
    }

    public static ApiException new403Forbidden(String messageFormat, Object... args) throws ApiException {
        return new ApiException(null, SC_403_FORBIDDEN, messageFormat, args);
    }

    public static ApiException new403Forbidden(Throwable cause, String messageFormat, Object... args) throws ApiException {
        return new ApiException(cause, SC_403_FORBIDDEN, messageFormat, args);
    }

    public static ApiException new404NotFound() throws ApiException {
        return new ApiException((Throwable)null, SC_404_NOT_FOUND, null);
    }

    public static ApiException new404NotFound(Throwable cause) throws ApiException {
        return new ApiException(cause, SC_404_NOT_FOUND, null);
    }

    public static ApiException new404NotFound(String messageFormat, Object... args) throws ApiException {
        return new ApiException(null, SC_404_NOT_FOUND, messageFormat, args);
    }

    public static ApiException new404NotFound(Throwable cause, String messageFormat, Object... args) throws ApiException {
        return new ApiException(cause, SC_404_NOT_FOUND, messageFormat, args);
    }

    public static ApiException new429TooManyRequests() throws ApiException {
        return new ApiException((Throwable)null, SC_429_TOO_MANY_REQUESTS, null);
    }

    public static ApiException new429TooManyRequests(Throwable cause) throws ApiException {
        return new ApiException(cause, SC_429_TOO_MANY_REQUESTS, null);
    }

    public static ApiException new429TooManyRequests(String messageFormat, Object... args) throws ApiException {
        return new ApiException(null, SC_429_TOO_MANY_REQUESTS, messageFormat, args);
    }

    public static ApiException new429TooManyRequests(Throwable cause, String messageFormat, Object... args) throws ApiException {
        return new ApiException(cause, SC_429_TOO_MANY_REQUESTS, messageFormat, args);
    }

    public static ApiException new500InternalServerError() throws ApiException {
        return new ApiException((Throwable)null, SC_500_INTERNAL_SERVER_ERROR, null);
    }

    public static ApiException new500InternalServerError(Throwable cause) throws ApiException {
        return new ApiException(cause, SC_500_INTERNAL_SERVER_ERROR, null);
    }

    public static ApiException new500InternalServerError(String messageFormat, Object... args) throws ApiException {
        return new ApiException(null, SC_500_INTERNAL_SERVER_ERROR, messageFormat, args);
    }

    public static ApiException new500InternalServerError(Throwable cause, String messageFormat, Object... args) throws ApiException {
        return new ApiException(cause, SC_500_INTERNAL_SERVER_ERROR, messageFormat, args);
    }

    public static ApiException new501NotImplemented() throws ApiException {
        return new ApiException((Throwable)null, SC_501_NOT_IMPLEMENTED, null);
    }

    public static ApiException new501NotImplemented(Throwable cause) throws ApiException {
        return new ApiException(cause, SC_501_NOT_IMPLEMENTED, null);
    }

    public static ApiException new501NotImplemented(String messageFormat, Object... args) throws ApiException {
        return new ApiException(null, SC_501_NOT_IMPLEMENTED, messageFormat, args);
    }

    public static ApiException new501NotImplemented(Throwable cause, String messageFormat, Object... args) throws ApiException {
        return new ApiException(cause, SC_501_NOT_IMPLEMENTED, messageFormat, args);
    }

}
