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

/**
 * Static constants for HTTP status codes.  Yes there are many other files in JDK-land
 * with this type of constant enumeration but I created this one because the other
 * usual suspect classed did not include the HTTP status number in the field name and
 * I really wanted to see both the numerical code and message at the same time in the source.
 *
 * @see <a href="http://www.restapitutorial.com/httpstatuscodes.html">Rest HTTP Status Codes</a>
 */
public interface Status {
    String SC_200_OK         = "200 OK";
    String SC_201_CREATED    = "201 Created";
    String SC_204_NO_CONTENT = "204 No Content";

    //@see https://developer.mozilla.org/en-US/docs/Web/HTTP/Redirections
    // String         SC_302_FOUND                 = "302 Found";
    String SC_308_PERMANENT_REDIRECT = "308 Permanent Redirect";

    String SC_400_BAD_REQUEST           = "400 Bad Request";
    String SC_401_UNAUTHORIZED          = "401 Unauthorized";
    String SC_403_FORBIDDEN             = "403 Forbidden";
    String SC_404_NOT_FOUND             = "404 Not Found";
    String SC_429_TOO_MANY_REQUESTS     = "429 Too Many Requests";
    String SC_500_INTERNAL_SERVER_ERROR = "500 Internal Server Error";
    String SC_501_NOT_IMPLEMENTED       = "501 Not Implemented";
}
