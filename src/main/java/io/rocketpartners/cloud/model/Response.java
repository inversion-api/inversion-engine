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
package io.rocketpartners.cloud.model;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Utils;

public class Response
{
   protected String                                 url               = null;

   protected Chain                                  chain             = null;

   protected ArrayListValuedHashMap<String, String> headers           = new ArrayListValuedHashMap();

   protected int                                    statusCode        = 200;
   protected String                                 statusMesg        = "OK";
   protected String                                 statusError       = null;
   protected String                                 redirect          = null;

   protected String                                 contentType       = null;
   protected StringBuffer                           out               = new StringBuffer();
   protected ObjectNode                             json              = new ObjectNode("meta", new ObjectNode("createdOn", Utils.formatIso8601(new Date())), "data", new ArrayNode());
   protected String                                 text              = null;

   protected String                                 fileName          = null;
   protected File                                   file              = null;

   protected Exception                              error             = null;

   protected String                                 contentRangeUnit  = null;
   protected long                                   contentRangeStart = -1;
   protected long                                   contentRangeEnd   = -1;
   protected long                                   contentRangeSize  = -1;

   protected List<Change>                           changes           = new ArrayList();
   protected StringBuffer                           debug             = new StringBuffer();

   public Response()
   {

   }

   public Response(String url)
   {
      withUrl(url);
   }

   public Response statusOk()
   {
      if (statusCode < 200 || statusCode > 299)
      {
         dump();
         throw new ApiException(statusCode + "", statusError);
      }

      return this;
   }

   public Response statusOk(String message)
   {
      if (statusCode < 200 || statusCode > 299)
         throw new ApiException(statusCode + "", message);

      return this;
   }

   public Response statusEq(int... statusCodes)
   {
      boolean matched = false;
      for (int statusCode : statusCodes)
      {
         if (statusCode == this.statusCode)
         {
            matched = true;
            break;
         }
      }

      if (!matched)
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Received unexpected status code '" + this.statusCode + "'");

      return this;
   }

   public Response statusEq(String message, int... statusCodes)
   {
      boolean matched = false;
      for (int statusCode : statusCodes)
      {
         if (statusCode == this.statusCode)
         {
            matched = true;
            break;
         }
      }

      if (!matched)
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, message);

      return this;
   }

   public Response statusEq(int statusCode, String message)
   {
      if (this.statusCode != statusCode)
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, message);

      return this;
   }

   public boolean hasStatus(int... statusCodes)
   {
      for (int statusCode : statusCodes)
      {
         if (this.statusCode == statusCode)
            return true;
      }
      return false;
   }

   public Response withMeta(String key, String value)
   {
      json.getNode("meta").put(key, value);
      return this;
   }

   public void write(StringBuffer buff, Object... msgs)
   {
      write0(buff, msgs);
      buff.append("\r\n");
   }

   protected void write0(StringBuffer buff, Object... msgs)
   {
      if (msgs != null && msgs.length == 0)
         return;

      if (msgs != null && msgs.length == 1 && msgs[0].getClass().isArray())
         msgs = (Object[]) msgs[0];

      for (int i = 0; msgs != null && i < msgs.length; i++)
      {
         Object msg = msgs[i];
         if (msg instanceof byte[])
            msg = new String((byte[]) msg);

         if (msg.getClass().isArray())
         {
            write0(buff, (Object[]) msg);
         }
         else
         {
            if (i > 0)
               buff.append(" ");
            buff.append(msg);
         }

      }
   }

   /**
    * @param statusCode - one of the SC constants ex "200 OK"
    */
   public Response withStatus(String status)
   {
      statusMesg = status;
      statusCode = Integer.parseInt(status.substring(0, 3));

      if (statusMesg.length() > 4)
      {
         statusMesg = status.substring(4, status.length());
      }

      return this;
   }

   public String getStatus()
   {
      return statusCode + " " + statusMesg;
   }

   public Response withStatusCode(int statusCode)
   {
      this.statusCode = statusCode;
      return this;
   }

   public Response withStatusMesg(String statusMesg)
   {
      this.statusMesg = statusMesg;
      return this;
   }

   public Chain getChain()
   {
      return chain;
   }

   public Service getService()
   {
      return chain != null ? chain.getService() : null;
   }

   public Response withChain(Chain chain)
   {
      this.chain = chain;
      return this;
   }

   public Response debug(Object... msgs)
   {
      write(debug, msgs);
      return this;
   }

   public Response out(Object... msgs)
   {
      debug(msgs);
      write(out, msgs);
      return this;
   }

   public Response withOutput(String output)
   {
      out = new StringBuffer(output);
      return this;
   }

   public String getOutput()
   {
      return out.toString();
   }

   public void dump()
   {
      System.out.println(getDebug());
   }

   public String getDebug()
   {
      return debug.toString();
   }

   public String getHeader(String key)
   {
      List<String> vals = headers.get(key);
      if (vals != null && vals.size() > 0)
         return vals.get(0);
      return null;
   }

   /**
    * @return the headers
    */
   public ArrayListValuedHashMap<String, String> getHeaders()
   {
      return headers;
   }

   public void withHeader(String key, String value)
   {
      if (!headers.containsMapping(key, value))
         headers.put(key, value);
   }

   /**
    * @return the json
    */
   public ObjectNode getJson()
   {
      if (file != null && file.length() > 0)
         return Utils.parseObjectNode(getContent());
      else
         return json;
   }

   /**
    * @deprecated this method sets the root json document you should use withData and withMeta
    * instead unless you REALLY want to change to wrapper document structure.
    * 
    * @param json the json to set
    */
   public Response withJson(ObjectNode json)
   {
      this.json = json;
      return this;
   }

   public String findString(String path)
   {
      return getJson().findString(path);
   }

   public int findInt(String path)
   {
      return getJson().findInt(path);
   }

   public boolean findBoolean(String path)
   {
      return getJson().findBoolean(path);
   }

   public ObjectNode findNode(String path)
   {
      return getJson().findNode(path);
   }

   public ArrayNode findArray(String path)
   {
      return getJson().findArray(path);
   }

   public Object find(String path)
   {
      return getJson().find(path);
   }

   public ArrayNode data()
   {
      return json.getArray("data");
   }

   public Response withData(ArrayNode data)
   {
      json.put("data", data);
      return this;
   }

   public Response withRecord(Object record)
   {
      data().add(record);
      return this;
   }

   public Response withRecords(List records)
   {
      for (Object record : records)
         data().add(record);
      return this;
   }

   public ObjectNode meta()
   {
      return json.getNode("meta");
   }

   public Response withMeta(String key, Object value)
   {
      meta().put(key, value);
      return this;
   }

   public Response withFoundRows(int foundRows)
   {
      withMeta("foundRows", foundRows);
      return this;
   }

   public int getFoundRows()
   {
      return findInt("meta.foundRows");
   }

   public Response withPageSize(int pageSize)
   {
      withMeta("pageSize", pageSize);
      return this;
   }

   public Response withPageNum(int pageNum)
   {
      withMeta("pageNum", pageNum);
      return this;
   }

   public Response withPageCount(int pageCount)
   {
      withMeta("pageCount", pageCount);
      return this;
   }

   public int getPageSize()
   {
      int pageSize = json.findInt("meta.pageSize");
      if (pageSize < 0)
      {
         Object arr = json.find("data.0.name");
         if (arr instanceof ArrayNode)
         {
            pageSize = ((ArrayNode) arr).size();
         }
      }
      return pageSize;
   }

   public String next()
   {
      return findString("meta.next");
   }

   public Response withNext(String nextPageUrl)
   {
      withMeta("next", nextPageUrl);
      return this;
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
      return text;
   }

   public Response withText(String text)
   {
      this.text = text;
      return this;
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

   public Response withRedirect(String redirect)
   {
      this.redirect = redirect;
      return this;
   }

   public String getContentType()
   {
      return contentType;
   }

   public Response withContentType(String contentType)
   {
      headers.remove("Content-Type");
      headers.put("Content-Type", contentType);
      this.contentType = contentType;
      return this;
   }

   public List<Change> getChanges()
   {
      return changes;
   }

   public Response withChanges(java.util.Collection<Change> changes)
   {
      this.changes.addAll(changes);
      return this;
   }

   public Response withChange(String method, String collectionKey, Object entityKey)
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
      return this;
   }

   public Response withChange(String method, String collectionKey, String... entityKeys)
   {
      for (int i = 0; entityKeys != null && i < entityKeys.length; i++)
         withChange(method, collectionKey, entityKeys[i]);
      return this;
   }

   public boolean isSuccess()
   {
      return statusCode >= 200 && statusCode <= 300 && error == null;
   }

   public Exception getError()
   {
      return error;
   }

   //   public String getLog()
   //   {
   //      return log;
   //   }

   //   public LinkedHashMap<String, String> getHeaders()
   //   {
   //      return new LinkedHashMap(headers);
   //   }

   //   public String getHeader(String header)
   //   {
   //      String value = headers.get(header);
   //      if (value == null)
   //      {
   //         for (String key : headers.keySet())
   //         {
   //            if (key.equalsIgnoreCase(header))
   //               return headers.get(key);
   //         }
   //      }
   //      return value;
   //   }

   public InputStream getInputStream() throws IOException
   {
      if (file != null)
         return new BufferedInputStream(new FileInputStream(file));

      return null;
   }

   public String getContent()
   {
      try
      {
         if (file != null && file.length() > 0)
         {
            String string = Utils.read(getInputStream());
            return string;
         }
         else if (json != null)
         {
            return json.toString();
         }
      }
      catch (Exception ex)
      {
         Utils.rethrow(ex);
      }
      return null;
   }

   public String getErrorContent()
   {
      if (!isSuccess() && error != null)
         return Utils.getShortCause(error);

      try
      {
         if (!isSuccess() && file != null && file.length() > 0)
         {
            String string = Utils.read(getInputStream());
            return string;
         }
      }
      catch (Exception ex)
      {
         Utils.rethrow(ex);
      }
      return null;
   }

   public long getFileLength()
   {
      if (file != null)
      {
         return file.length();
      }
      return -1;
   }

   public Response withFile(File file) throws Exception
   {
      this.file = file;
      return this;
   }

   public File getFile()
   {
      return file;
   }

   /**
    * This is the value returned from the server via the "Content-Length" header
    * NOTE: this will not match file length, for partial downloads, consider also using ContentRangeSize
    * @return
    */
   public long getContentLength()
   {
      String length = getHeader("Content-Length");
      if (length != null)
      {
         return Long.parseLong(length);
      }
      return 0;
   }

   /**
    * This value come from the "Content-Range" header and is the unit part
    * Content-Range: <unit> <range-start>-<range-end>/<size>
    * @return
    */
   public String getContentRangeUnit()
   {
      parseContentRange();
      return contentRangeUnit;
   }

   /**
    * This value come from the "Content-Range" header and is the first part
    * Content-Range: <unit> <range-start>-<range-end>/<size>
    * @return
    */
   public long getContentRangeStart()
   {
      parseContentRange();
      return contentRangeStart;
   }

   /**
    * This value come from the "Content-Range" header and is the middle part
    * Content-Range: <unit> <range-start>-<range-end>/<size>
    * @return
    */
   public long getContentRangeEnd()
   {
      parseContentRange();
      return contentRangeEnd;
   }

   /**
    * This value come from the "Content-Range" header and is the last part
    * Content-Range: <unit> <range-start>-<range-end>/<size>
    * @return
    */
   public long getContentRangeSize()
   {
      parseContentRange();
      return contentRangeSize;
   }

   /**
    * Parses the "Content-Range" header
    * Content-Range: <unit> <range-start>-<range-end>/<size>
    */
   private void parseContentRange()
   {
      if (contentRangeUnit == null)
      {
         String range = getHeader("Content-Range");
         if (range != null)
         {
            String[] parts = range.split(" ");
            contentRangeUnit = parts[0];
            parts = parts[1].split("/");
            contentRangeSize = Long.parseLong(parts[1]);
            parts = parts[0].split("-");
            if (parts.length == 2)
            {
               contentRangeStart = Long.parseLong(parts[0]);
               contentRangeEnd = Long.parseLong(parts[1]);
            }
         }
      }
   }

   public Response withUrl(String url)
   {
      if (!Utils.empty(url))
      {
         url = url.trim();
         url = url.replaceAll(" ", "%20");
      }

      this.url = url;

      if (Utils.empty(fileName))
      {
         try
         {
            fileName = new URL(url).getFile();
            if (Utils.empty(fileName))
               fileName = null;
         }
         catch (Exception ex)
         {

         }
      }
      return this;
   }

   public Response withError(Exception ex)
   {
      this.error = ex;
      return this;
   }

   public String getFileName()
   {
      return fileName;
   }

   public String getUrl()
   {
      return url;
   }

   //   public Response onSuccess(ResponseHandler handler)
   //   {
   //      if (isSuccess())
   //      {
   //         try
   //         {
   //            handler.onResponse(this);
   //         }
   //         catch (Exception ex)
   //         {
   //            logger.error("Error handling onSuccess", ex);
   //         }
   //      }
   //      return this;
   //   }
   //
   //   public Response onFailure(ResponseHandler handler)
   //   {
   //      if (!isSuccess())
   //      {
   //         try
   //         {
   //            handler.onResponse(this);
   //         }
   //         catch (Exception ex)
   //         {
   //            logger.error("Error handling onFailure", ex);
   //         }
   //      }
   //      return this;
   //   }
   //
   //   public Response onResponse(ResponseHandler handler)
   //   {
   //      try
   //      {
   //         handler.onResponse(this);
   //      }
   //      catch (Exception ex)
   //      {
   //         logger.error("Error handling onResponse", ex);
   //      }
   //      return this;
   //   }

   @Override
   public String toString()
   {
      return debug.toString();
   }

   @Override
   public void finalize()
   {
      if (file != null)
      {
         try
         {
            File tempFile = file;
            file = null;
            tempFile.delete();
         }
         catch (Throwable t)
         {
            // ignore
         }
      }
   }

}