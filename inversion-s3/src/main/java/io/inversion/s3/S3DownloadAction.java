/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
package io.inversion.cloud.action.s3;

import io.inversion.cloud.model.Action;

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
 * Mar 6, 2019 - If a json body is received, it is expected that a meta update should
 * occur.  If a multipart form is received, it is expected that a binary file
 * was sent and possibly json
 * 
 * @author kfrankic
 *
 */
public class S3DownloadAction extends Action
{
//   protected transient Logger log     = LoggerFactory.getLogger(S3DbRestHandler.class);
//
//   int    maxRows = 100;
//
//   @Override
//   public void service(Engine service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
//   {
//      String method = req.getMethod();
//      if ("GET".equalsIgnoreCase(method))
//      {
//         doGet(service, api, endpoint, action, chain, req, res);
//      }
//      else if ("POST".equalsIgnoreCase(method))
//      {
//         doPost(service, api, endpoint, action, chain, req, res);
//      }
//      else if ("PUT".equalsIgnoreCase(method))
//      {
//         doPut(service, api, endpoint, action, chain, req, res);
//      }
//      else
//      {
//         throw new ApiException(SC.SC_400_BAD_REQUEST, "The S3 handler only supports GET and POST/PUT requests");
//      }
//   }
//
//   /**
//    * All objects within a bucket can be retrieved by hitting the bucket without any parameters.
//    * http://hostname/us/s3/bucketName
//    * 
//    * To narrow a result list, a prefix can be specified.  Only objects within the bucket with the specified prefix
//    * will be returned.  To limit the results to those within a 'folder' the prefix include the folder name and end 
//    * with a '/'.  
//    * http://hostname/us/s3/bucketName?sw(key,someFolder/)
//    * The following example would return all objects within 'someFolder' that start with 'xyz'
//    * http://hostname/us/s3/bucketName?sw(key,someFolder/xyz)
//    *
//    * To obtain 'extended' meta of an object, two different methods can be used.  A request with the 
//    * following params: sw(key,helloThere/)&eq(key,filename.json) is the same as a request with only 
//    * eq(key,helloThere/filename.json)
//    * 
//    * To download an object, the same request as retrieving 'extended' meta is used, but a 'download'
//    * parameter must be included.
//    * 
//    */
//   public void doGet(Engine service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
//   {
//
//      Collection collection = findCollectionOrThrow404(api, chain, req);
//      Table table = collection.getEntity().getTable();
//      S3Db db = (S3Db) table.getDb();
//
//      Integer pageSize = req.getParam("pageSize") != null ? Integer.parseInt(req.removeParam("pageSize")) : maxRows;
//
//      S3Rql rql = (S3Rql) Rql.getRql(db.getType());
//      S3Request s3Req = rql.buildS3Request(req, table, pageSize);
//
//      ObjectMapper mapper = new ObjectMapper();
//
//      if (s3Req.isDownload())
//      {
//         // path == /s3/bucketName?eq(key,filename)&download
//
//         // If a prefix exists, it must be tacked onto the key.
//         S3Object s3File = db.getDownload(s3Req);
//
//         // relies on Servlet to close the stream.
//         res.setInputStream(s3File.getObjectContent());
//         res.setContentType(s3File.getObjectMetadata().getContentType());
//
//         res.addHeader("Content-Type", s3File.getObjectMetadata().getContentType());
//         res.addHeader("Content-Length", Long.toString(s3File.getObjectMetadata().getContentLength()));
//
//      }
//      else if (s3Req.getKey() != null)
//      {
//         // Attempt to retrieve the extended meta for the key.
//         // If that does not exist, attempt to get a list of objects using the key as a prefix.
//
//         // path == /s3/bucketName?eq(key,filename)
//         // path == /s3/bucketName/key
//         // retrieve the extended meta data of a file.
//
//         ObjectNode json = null;
//
//         try
//         {
//            ObjectMetadata meta = db.getExtendedMetaData(s3Req);
//
//            json = JS.toObjectNode(mapper.writeValueAsString(meta));
//            String pathPrefix = req.getPath().substring(0, req.getPath().indexOf(req.getSubpath()));
//            json.put("href", req.getApiUrl() + pathPrefix + s3Req.getBucket() + "/" + s3Req.getKey());
//
//            res.setJson(json);
//         }
//         catch (Exception e)
//         {
//            log.warn("Attempting to retrieve as list after failing to obtain extended meta for key: " + s3Req.getKey());
//         }
//
//         if (json == null)
//         {
//            // TODO is there a way to prevent the req from adding the '/' onto it's path?
//
//            // The key does not exist.  Perhaps the request was intended to be for an objects listing...
//            // FYI: below, a '/' is added to the prefix for two reasons:
//            // 1) by default, Inversion adds a '/' to the end of the path which is then removed during stmt creation,
//            // so we dont know if the '/' is intended or not.
//            // 2) if a '/' is NOT tacked onto the prefix, then 'this' directory will be returned as a prefix along with all files
//            // that start with this prefix...meaning, NO inner directories or files will be returned.
//            // To work around this limitation, if the user wants to specify a directory & file prefix, the 'sw' function should 
//            // be used.  ex: sw(key,media/c) will return all files/directories that are within the media folder and start with 'c'
//            getObjectsList(req, res, new S3Request(s3Req.getBucket(), s3Req.getKey() + "/", null, s3Req.getSize(), false, s3Req.getMarker()), db, mapper);
//         }
//
//      }
//      else
//      {
//         getObjectsList(req, res, s3Req, db, mapper);
//      }
//
//      res.setStatus(SC.SC_200_OK);
//   }
//
//   private void getObjectsList(Request req, Response res, S3Request s3Req, S3Db db, ObjectMapper mapper) throws Exception
//   {
//
//
//   }
//
//   /**
//    * Use the 'sw' function to post a file to a specific location within a bucket; ex: sw(key, media/).  
//    * Otherwise, the file will be saved to the specified bucket's root.  By default, the uploaded file 
//    * name will be used when storing the file to s3.  A file name can specified by using the 'eq' function;
//    * ex: eq(key, newFile.name)
//    * 
//    * Custom metadata can be added to the file by setting a 'header prefix' value during configuration of 
//    * this class.  All headers sent by the client that use this prefix will be applied to the s3 object's
//    * custom metadata.
//    * 
//    * **Note** That user-metadata for an object is limited by the HTTP requestheader limit. All HTTP 
//    * headers included in a request (including usermetadata headers and other standard HTTP headers) must 
//    * be less than 8KB
//    * 
//    * **NOTE** A 'header prefix' AND custom Content-Type header MUST be applied if you want the correct
//    * content-type applied to the s3 object.  ex: headerPrefix = "s3-";  The request header would include:
//    * s3-Content-Type=application/json if you wanted an object stored with a json content type.
//    */
//   private void doPost(Engine service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
//   {
//
//      Collection collection = findCollectionOrThrow404(api, chain, req);
//      Table table = collection.getEntity().getTable();
//      S3Db db = (S3Db) table.getDb();
//
//      S3Rql rql = (S3Rql) Rql.getRql(db.getType());
//      S3Request s3Req = rql.buildS3Request(req, table, null);
//
//      String key = s3Req.getKey();
//
//      List<Upload> uploads = req.getUploads();
//
//      if (uploads.size() > 0)
//      {
//         DigestInputStream uploadStream = null;
//         Upload upload = uploads.get(0);
//
//         uploadStream = new DigestInputStream(upload.getInputStream(), MessageDigest.getInstance("MD5"));
//
//         ObjectMetadata meta = new ObjectMetadata();
//
//         String metaParam = req.getParam("meta");
//
//         // set custom metadata for the file
//         if (metaParam != null)
//         {
//            ObjectNode metaJs = JS.toObjectNode(metaParam);
//            Map<String, String> metaMap = metaJs.asMap();
//
//            for (Map.Entry<String, String> entry : metaMap.entrySet())
//            {
//               String metaKey = entry.getKey();
//
//               if (metaKey.equalsIgnoreCase("content-type"))
//               {
//                  meta.setContentType(entry.getValue());
//               }
//               else if (metaKey.equalsIgnoreCase("name"))
//               {
//                  key = entry.getValue();
//               }
//               else
//               {
//                  meta.addUserMetadata(metaKey, entry.getValue());
//               }
//            }
//
//         }
//
//         meta.setContentLength(upload.getFileSize());
//
//         PutObjectResult result = db.saveFile(uploadStream, s3Req.getBucket(), key, meta);
//         if (result == null)
//            throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Failed to POST/PUT file to s3: " + key);
//
//         // not including the result object as it contains confusing/pointless data.
//         // such as a 'content-length' of 0, because it's the content-length of the response, not the 
//         // size of the upload.
//         ObjectNode json = new ObjectNode();
//
//         json.put("href", req.getApiUrl() + req.getPath() + key);
//
//         res.setJson(json);
//
//      }
//
//      res.setStatus(SC.SC_200_OK);
//   }
//
//   private void doPut(Engine service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
//   {
//      Collection collection = findCollectionOrThrow404(api, chain, req);
//      Table table = collection.getEntity().getTable();
//      S3Db db = (S3Db) table.getDb();
//
//      // Only be updating the meta of a file at this time.  Renaming or moving a file 
//      // should also be handled by db.updateObject() but neither are currently implemented.
//
//      ObjectNode metaJson = req.getJson();
//
//      String key = null;
//      try
//      {
//         key = metaJson.getString("name");
//      }
//      catch (Exception e)
//      {
//         throw new ApiException("When updating metadata, a 'name' must be specified");
//      }
//
//      // All previous metadata will be wiped out.
//      ObjectMetadata meta = buildMetadata(metaJson);
//
//      CopyObjectResult copy = db.updateObject(table.getName(), key, table.getName(), key, meta);
//      ObjectMapper mapper = new ObjectMapper();
//
//      // the copy result doesn't contain much helpful data.
//      ObjectNode json = JS.toObjectNode(mapper.writeValueAsString(copy));
//
//      json.put("href", req.getApiUrl() + req.getPath() + key);
//
//      res.setJson(json);
//      res.setStatus(SC.SC_200_OK);
//
//   }
//
//   private Collection findCollectionOrThrow404(Api api, Chain chain, Request req) throws Exception
//   {
//      Collection collection = api.getCollection(req.getCollectionKey(), S3Db.class);
//
//      if (collection == null)
//      {
//         throw new ApiException(SC.SC_404_NOT_FOUND, "An s3 bucket is not configured for this collection key, please edit your query or your config and try again.");
//      }
//
//      if (!(collection.getEntity().getTable().getDb() instanceof S3Db))
//      {
//         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Bad server configuration. The endpoint is hitting the s3 handler, but this collection is not related to a s3db");
//      }
//
//      return collection;
//   }
//
//   private ObjectMetadata buildMetadata(ObjectNode metaJs)
//   {
//      ObjectMetadata meta = null;
//
//      if (metaJs != null)
//      {
//         // All previous metadata will be wiped out.
//         meta = new ObjectMetadata();
//
//         Map<String, String> metaMap = metaJs.asMap();
//
//         for (Map.Entry<String, String> entry : metaMap.entrySet())
//         {
//            String metaKey = entry.getKey();
//
//            switch (metaKey.toLowerCase())
//            {
//               case "content-type":
//                  meta.setContentType(entry.getValue());
//                  break;
//               case "name":
//               case "tenantid":
//                  break;
//               default :
//                  meta.addUserMetadata(metaKey, entry.getValue());
//            }
//         }
//      }
//
//      return meta;
//   }
//
//   private ObjectNode buildListObj(String href, Date lastModified, Long size, boolean isFile)
//   {
//      ObjectNode jsObj = new ObjectNode();
//      jsObj.put("href", href);
//      if (lastModified != null)
//         jsObj.put("lastModified", lastModified);
//      if (size != null)
//         jsObj.put("size", size);
//      jsObj.put("isFile", isFile);
//
//      return jsObj;
//   }
//
//   public static void main(String[] args)
//   {
//      System.out.println("hello".compareToIgnoreCase("world")); // -15
//      System.out.println("world".compareToIgnoreCase("hello")); // 15
//   }

}
