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

import io.forty11.js.JSObject;
import io.forty11.utils.ListMap;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

public class Response
{

   HttpServletResponse     httpResp    = null;
   ListMap<String, String> headers     = new ListMap();
   int                     statusCode  = 200;
   String                  statusMesg  = "OK";
   String                  statusError = null;
   JSObject                json        = new JSObject();
   String                  text        = null;

   StringBuffer            debug       = null;

   
   public Response() throws Exception
   {
      
   }
   
   public Response(HttpServletResponse httpResp) throws Exception
   {
      this.httpResp = httpResp;
   }
   

   public String getDebug()
   {
      return debug != null ? debug.toString() : null;
   }

   public void debug(Object... msgs)
   {
      if (debug == null)
         debug = new StringBuffer();

      for (int i = 0; msgs != null && i < msgs.length; i++)
      {
         Object msg = msgs[i];
         if (msg instanceof byte[])
            msg = new String((byte[]) msg);

         debug.append(msg).append("\r\n");
      }
   }

   /**
    * @param statusCode - one of the SC constants ex "200 OK"
    */
   public void setStatus(String status)
   {
      statusMesg = status;
      statusCode = Integer.parseInt(status.substring(0, 3));

      if (statusMesg.length() > 4)
      {
         statusMesg = status.substring(4, status.length());
      }
   }

   /**
    * @return the headers
    */
   public ListMap<String, String> getHeaders()
   {
      return headers;
   }

   public void addHeader(String key, String value)
   {
      if (!headers.containsKey(key, value))
         headers.put(key, value);
   }

   /**
    * @return the json
    */
   public JSObject getJson()
   {
      return json;
   }

   /**
    * @param json the json to set
    */
   public void setJson(JSObject json)
   {
      this.json = json;
   }

   /**
    * @return the httpResp
    */
   public HttpServletResponse getHttpResp()
   {
      return httpResp;
   }

   /**
    * @return the statusMesg
    */
   public String getStatusMesg()
   {
      return statusMesg;
   }

   /**
    * @return the statusCode
    */
   public int getStatusCode()
   {
      return statusCode;
   }

   public String getText()
   {
      if(text == null && json != null)
         return json.toString();
      
      return text;
   }

   public void setText(String text)
   {
      this.text = text;
   }
}