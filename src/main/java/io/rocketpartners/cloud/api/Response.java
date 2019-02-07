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
package io.rocketpartners.cloud.api;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import io.rocketpartners.J;
import io.rocketpartners.utils.ListMap;
import io.rocketpartners.rest.JSObject;

public class Response
{
   ListMap<String, String> headers     = new ListMap();

   int                     statusCode  = 200;
   String                  statusMesg  = "OK";
   String                  statusError = null;
   String                  redirect    = null;
   JSObject                json        = new JSObject();
   String                  text        = null;
   String                  contentType = null;
   List<Change>            changes     = new ArrayList();

   StringBuffer            debug       = new StringBuffer();

   StringBuffer            out         = new StringBuffer();

   public Response()
   {

   }

   public void write(StringBuffer buff, Object... msgs)
   {
      for (int i = 0; msgs != null && i < msgs.length; i++)
      {
         Object msg = msgs[i];
         if (msg instanceof byte[])
            msg = new String((byte[]) msg);

         buff.append(msg).append("\r\n");
      }
   }

   public void debug(Object... msgs)
   {
      write(debug, msgs);
   }

   public void out(Object... msgs)
   {
      debug(msgs);
      write(out, msgs);
   }

   public void setOutput(String output)
   {
      out = new StringBuffer(output);
   }

   public String getOutput()
   {
      return out.toString();
   }

   public String getDebug()
   {
      return debug.toString();
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

   public String getStatus()
   {
      return statusCode + " " + statusMesg;
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

   //   /**
   //    * @return the httpResp
   //    */
   //   public HttpServletResponse getHttpResp()
   //   {
   //      return httpResp;
   //   }

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
      return text;
   }

   public void setText(String text)
   {
      this.text = text;
   }

   public String getEntityKey()
   {
      if (json != null)
      {
         String href = json.getString("href");
         if (href != null)
         {
            String[] parts = href.split("/");
            return parts[parts.length - 1];
         }
      }
      return null;
   }

   public String getRedirect()
   {
      return redirect;
   }

   public void setRedirect(String redirect)
   {
      this.redirect = redirect;
   }

   public String getContentType()
   {
      return contentType;
   }

   public void setContentType(String contentType)
   {
      headers.remove("Content-Type");
      headers.put("Content-Type", contentType);
      this.contentType = contentType;
   }

   public List<Change> getChanges()
   {
      return changes;
   }

   public void addChanges(java.util.Collection<Change> changes)
   {
      this.changes.addAll(changes);
   }

   public void addChange(String method, String collectionKey, Object entityKey)
   {
      if (entityKey instanceof List)
      {
         List<String> deletedIds = (List<String>) entityKey;
         for (String id : deletedIds)
         {
            changes.add(new Change(method, collectionKey, id));
         }
      }
      else
      {
         changes.add(new Change(method, collectionKey, entityKey));
      }
   }

   public void addChanges(String method, String collectionKey, String... entityKeys)
   {
      for (int i = 0; entityKeys != null && i < entityKeys.length; i++)
         addChange(method, collectionKey, entityKeys[i]);
   }
}