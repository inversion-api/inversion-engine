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
package io.rcktapp.api.handler.s3;

import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.forty11.web.js.JS;
import io.forty11.web.js.JSObject;
import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Collection;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Request;
import io.rcktapp.api.Request.Upload;
import io.rcktapp.api.Response;
import io.rcktapp.api.SC;
import io.rcktapp.api.Table;
import io.rcktapp.api.service.Service;
import io.rcktapp.rql.Rql;
import io.rcktapp.rql.s3.S3Rql;
import io.rcktapp.utils.CaseInsensitiveLookupMap;

/**
 * Accepts RQL parameters and responds with json or files to the client.
 * Special request parameters used by the GET handler: 
 * 'download' attempts to download the specified key.
 * 'marker' determines where paging should begin
 * 
 * Supports simple RQL functions: eq & sw
 * 
 * TODO it would be awesome if a user could request several files to be downloaded.
 * The files would be zipped and returned to the client.  A zip would be named
 * either 'files.zip' for various files, or 'sw_x_files.zip' where files that
 * 'start with' x are zipped.
 * 
 * TODO what to do about buckets containing '.'s within the name? ex:
 * Missing parent for map compression: api.collections.s3db_files.liftck.coms
 * Missing parent for map compression: api.collections.s3db_static-pages.liftck.coms
 * Missing parent for map compression: s3db.tables.files.liftck.com
 * Missing parent for map compression: s3db.tables.static-pages.liftck.com
 * 
 * @author kfrankic
 *
 */
public class S3DbRestHandler implements Handler
{
   Logger log          = LoggerFactory.getLogger(S3DbRestHandler.class);

   int    maxRows      = 100;
   String headerPrefix = null;

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      String method = req.getMethod();
      if ("GET".equalsIgnoreCase(method))
      {
         doGet(service, api, endpoint, action, chain, req, res);
      }
      else if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method))
      {
         doPost(service, api, endpoint, action, chain, req, res);
      }
      else
      {
         throw new ApiException(SC.SC_400_BAD_REQUEST, "The S3 handler only supports GET and POST/PUT requests");
      }
   }

   /**
    * All objects within a bucket can be retrieved by hitting the bucket without any parameters.
    * http://hostname/us/s3/bucketName
    * 
    * To narrow a result list, a prefix can be specified.  Only objects within the bucket with the specified prefix
    * will be returned.  To limit the results to those within a 'folder' the prefix include the folder name and end 
    * with a '/'.  
    * http://hostname/us/s3/bucketName?sw(key,someFolder/)
    * The following example would return all objects within 'someFolder' that start with 'xyz'
    * http://hostname/us/s3/bucketName?sw(key,someFolder/xyz)
    *
    * To obtain 'extended' meta of an object, two different methods can be used.  A request with the 
    * following params: sw(key,helloThere/)&eq(key,filename.json) is the same as a request with only 
    * eq(key,helloThere/filename.json)
    * 
    * To download an object, the same request as retrieving 'extended' meta is used, but a 'download'
    * parameter must be included.
    * 
    */
   public void doGet(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {

      Collection collection = findCollectionOrThrow404(api, chain, req);
      Table table = collection.getEntity().getTable();
      S3Db db = (S3Db) table.getDb();

      Integer pageSize = req.getParam("pageSize") != null ? Integer.parseInt(req.removeParam("pageSize")) : maxRows;

      boolean isDownloadRequest = req.removeParam("download") != null ? true : false;
      String marker = req.removeParam("marker");

      S3Rql rql = (S3Rql) Rql.getRql(db.getType());
      S3Request s3Req = rql.buildS3Request(req.getParams(), table, pageSize);

      ObjectMapper mapper = new ObjectMapper();

      if (isDownloadRequest)
      {
         // path == /s3/bucketName?eq(key,filename)&download

         // If a prefix exists, it must be tacked onto the key.
         S3Object s3File = db.getDownload(s3Req);

         // relies on Servlet to close the stream.
         res.setInputStream(s3File.getObjectContent());
         res.setContentType(s3File.getObjectMetadata().getContentType());

         res.addHeader("Content-Type", s3File.getObjectMetadata().getContentType());
         res.addHeader("Content-Length", Long.toString(s3File.getObjectMetadata().getContentLength()));

      }
      else if (!isDownloadRequest && s3Req.getKey() != null)
      {
         // path == /s3/bucketName?eq(key,filename)
         // retrieve the extended meta data of a file.

         ObjectMetadata meta = db.getExtendedMetaData(s3Req);

         JSObject json = new JSObject();

         json.put("meta", new JSObject());
         json.put("data", JS.toJSObject(mapper.writeValueAsString(meta)));

         res.setJson(json);
      }
      else
      {
         // path == /s3/bucketName with no params
         // retrieve as much meta data as possible about the files in the bucket

         ObjectListing listing = db.getCoreMetaData(s3Req, marker);

         JSObject json = new JSObject();

         // standard Inversion meta includes:
         // "rowCount": x, - there is currently no way of knowing this.
         // "pageNum": x, - can't know.
         // "pageSize": x, - know.
         // "pageCount": x, - can't know.
         // "prev": null, - could know, if passed as req param.
         // "next": "http://localhost:8080/api/lift/us/elastic/ads?&pageSize=100&sort=id&source=id,json.id,json.modifiedat&pageNum=2"
         JSObject jsMeta = new JSObject();
         jsMeta.put("pageSize", listing.getMaxKeys());
         jsMeta.put("prev", null);
         String nextMarker = "";
         if (listing.isTruncated())
         {
            String query = req.getUrl().getQuery();
            nextMarker = (query.length() == 0 ? ("?marker=" + listing.getNextMarker()) : ("&marker=" + listing.getNextMarker()));
         }
         jsMeta.put("next", listing.isTruncated() ? req.getUrl().toString() + nextMarker : null);
         json.put("meta", jsMeta);

         JSObject jsData = new JSObject();
         jsData.put("directories", JS.toJSObject(mapper.writeValueAsString(listing.getCommonPrefixes())));
         jsData.put("files", JS.toJSObject(mapper.writeValueAsString(listing.getObjectSummaries())));
         json.put("data", jsData);

         res.setJson(json);
      }

      res.setStatus(SC.SC_200_OK);
   }

   /**
    * Use the 'sw' function to post a file to a specific location within a bucket; ex: sw(key, media/).  
    * Otherwise, the file will be saved to the specified bucket's root.  By default, the uploaded file 
    * name will be used when storing the file to s3.  A file name can specified by using the 'eq' function;
    * ex: eq(key, newFile.name)
    * 
    * Custom metadata can be added to the file by setting a 'header prefix' value during configuration of 
    * this class.  All headers sent by the client that use this prefix will be applied to the s3 object's
    * custom metadata.
    * 
    * **NOTE**: A 'header prefix' AND custom Content-Type header MUST be applied if you want the correct
    * content-type applied to the s3 object.  ex: headerPrefix = "s3-";  The request header would include:
    * s3-Content-Type=application/json if you wanted an object stored with a json content type.
    */
   private void doPost(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {

      Collection collection = findCollectionOrThrow404(api, chain, req);
      Table table = collection.getEntity().getTable();
      S3Db db = (S3Db) table.getDb();

      S3Rql rql = (S3Rql) Rql.getRql(db.getType());
      S3Request s3Req = rql.buildS3Request(req.getParams(), table, null);

      String prefix = s3Req.getPrefix();
      String key = s3Req.getKey();

      if (!prefix.endsWith("/"))
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "S3 RQL 'sw' value should end with a '/' when attempting to save a file.");

      List<Upload> uploads = req.getUploads();

      String fileName = null;
      DigestInputStream uploadStream = null;

      if (uploads.size() > 0)
      {
         Upload upload = uploads.get(0);

         uploadStream = new DigestInputStream(upload.getInputStream(), MessageDigest.getInstance("MD5"));
         if (key != null)
            fileName = prefix != null ? prefix + key : key;
         else
            fileName = prefix != null ? prefix + upload.getFileName() : upload.getFileName();

         ObjectMetadata meta = new ObjectMetadata();

         // set custom metadata for the file
         if (headerPrefix != null)
         {
            CaseInsensitiveLookupMap<String, String> reqHeadersMap = req.getPrefixedHeaders(headerPrefix);
            int prefixEnd = headerPrefix.length();
            for (Map.Entry<String, String> entry : reqHeadersMap.entrySet())
            {

               String header = entry.getKey().substring(prefixEnd);

               if (header.equalsIgnoreCase("Content-Type"))
               {
                  meta.setContentType(entry.getValue());
               }
               else
               {
                  meta.addUserMetadata(header, entry.getValue());
               }
            }

         }

         meta.setContentLength(upload.getFileSize());

         PutObjectResult result = db.saveFile(uploadStream, s3Req.getBucket(), fileName, meta);
         if (result == null)
            throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Failed to POST/PUT file to s3: " + fileName);
      }

      res.setStatus(SC.SC_200_OK);
   }



   private Collection findCollectionOrThrow404(Api api, Chain chain, Request req) throws Exception
   {
      Collection collection = api.getCollection(req.getCollectionKey(), S3Db.class);

      if (collection == null)
      {
         throw new ApiException(SC.SC_404_NOT_FOUND, "An s3 bucket is not configured for this collection key, please edit your query or your config and try again.");
      }

      if (!(collection.getEntity().getTable().getDb() instanceof S3Db))
      {
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Bad server configuration. The endpoint is hitting the s3 handler, but this collection is not related to a s3db");
      }

      if (req.getSubpath().split("/").length > 1)
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "S3 RQL will not interpret the subpath after the specified collection key: " + req.getCollectionKey());

      return collection;
   }

}
