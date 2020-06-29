/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
package io.inversion.s3;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import io.inversion.Action;
import io.inversion.ApiException;
import io.inversion.Chain;
import io.inversion.Request;
import io.inversion.Request.Upload;
import io.inversion.Response;
import io.inversion.Status;
import io.inversion.utils.JSNode;
import io.inversion.utils.Utils;

/**
 * Sends browser multi-part file uploads to a defined S3 location
 * 
 * Bean property config can be set directly on the handler in your
 * inversion.properties files but you should really consider this to be
 * a service singleton where the properties can be passed in via 
 * Action config allowing a single handler instance to upload files
 * to multiple buckets based on request path.
 * 
 * So instead of config-ing something like
 * 
 *   handler.dynamicBasePath=yyyy/MM/dd
 *   or
 *   handler.bucket=somebucket
 *   
 * do this
 * 
 *   action.config=dynamicBasePath=yyyy/MM/dd&bucket=somebucket
 * 
 * While accessKey/secreKey/awsRegion CAN be set either on the Handler
 * or on the Action in this way, if you control the host environment
 * and are uploading everyting to your own AWS account, you should 
 * consider using IAM roles to authenticate.  Than way you don't need
 * to config the credentials at all.
 *
 */
public class S3UploadAction extends Action<S3UploadAction> {

   protected String s3AccessKey = null;
   protected String s3SecretKey = null;
   protected String s3AwsRegion = null;

   protected String s3Bucket    = null;
   protected String s3BasePath  = "uploads";
   protected String s3DatePath  = "yyyy/MM/dd";

   @Override
   public void run(Request req, Response res) throws ApiException {
      String requestPath = null;
      String fileName = null;
      Long fileSize = null;
      DigestInputStream uploadStream = null;

      try {
         List<Upload> uploads = req.getUploads();
         if (uploads.size() > 0) {
            Upload upload = uploads.get(0);

            uploadStream = new DigestInputStream(upload.getInputStream(), MessageDigest.getInstance("MD5"));
            String[] fileNameParts = upload.getFileName().split("[.]");
            fileName = "" + fileNameParts[0] + "-" + System.currentTimeMillis() + "." + fileNameParts[1];
            fileSize = upload.getFileSize();

            requestPath = upload.getRequestPath();
            if (requestPath.indexOf("/") == 0)
               requestPath = requestPath.substring(1);
         }

         if (uploadStream == null) {
            error(res, null, "No file was uploaded in the multipart request");
            return;
         }

         //String pathAndFileName = buildFullPath(fileName, requestPath);
         Map<String, Object> responseContent = new HashMap<>();

         try {
            responseContent = saveFile(req.getChain(), uploadStream, fileName, requestPath);
         } catch (Exception e) {
            error(res, e, "S3 Key may be invalid - valid characters are [  0-9 a-z A-Z !-_.*'()  ] --- your requested key was: " + requestPath + "/" + fileName);

            return;
         }

         responseContent.put("fileMd5", getHash(uploadStream.getMessageDigest()));
         responseContent.put("fileSizeBytes", fileSize);
         res.withJson(new JSNode(responseContent));
      } catch (Exception ex) {
         ApiException.throw500InternalServerError(ex);
      } finally {
         if (uploadStream != null) {
            Utils.close(uploadStream);
         }
      }
   }

   void error(Response res, Exception exception, String message) {
      if (exception != null)
         message += "\r\n\r\n" + Utils.getShortCause(exception);

      res.withStatus(Status.SC_400_BAD_REQUEST);
      Map<String, String> content = new HashMap<>();
      content.put("message", message);
      content.put("error", "Bad Request Exception");
      res.withJson(new JSNode(content));

   }

   private Map<String, Object> saveFile(Chain chain, InputStream inputStream, String fileName, String requestPath) throws Exception {
      AmazonS3 s3 = buildS3Client(chain);
      String bucket = chain.getConfig("s3Bucket", this.s3Bucket);
      String pathAndFileName = buildFullPath(chain, requestPath, fileName);

      s3.putObject(new PutObjectRequest(bucket, pathAndFileName, inputStream, new ObjectMetadata()));

      Map<String, Object> resp = new HashMap<>();
      resp.put("url", "http://" + bucket + ".s3.amazonaws.com/" + pathAndFileName);
      resp.put("fileName", fileName);
      resp.put("path", pathAndFileName);

      return resp;
   }

   private String buildFullPath(Chain chain, String requestPath, String name) {
      StringBuilder sb = new StringBuilder();

      String basePath = chain.getConfig("s3BasePath", this.s3BasePath);
      String datePath = chain.getConfig("s3DatePath", this.s3DatePath);

      if (basePath != null) {
         sb.append(basePath);
         if (!basePath.endsWith("/")) {
            sb.append("/");
         }
      }

      if (datePath != null) {
         datePath = new SimpleDateFormat(datePath).format(new Date());
         sb.append(datePath + "/");
      }

      if (requestPath != null) {
         if (requestPath.startsWith("/"))
            sb.append(requestPath.substring(1));
         else
            sb.append(requestPath);
         if (!requestPath.endsWith("/"))
            sb.append("/");
      }
      sb.append(name);
      return sb.toString();
   }

   private AmazonS3 buildS3Client(Chain chain) {
      //TODO make this work like dynamo client config as art of db

      String accessKey = chain.getConfig("s3AccessKey", this.s3AccessKey);
      String secretKey = chain.getConfig("s3SecretKey", this.s3SecretKey);
      String awsRegion = chain.getConfig("s3AwsRegion", this.s3AwsRegion);

      AmazonS3ClientBuilder builder = null;
      if (accessKey != null) {
         BasicAWSCredentials creds = new BasicAWSCredentials(accessKey, secretKey);
         builder = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(creds));
      } else {
         builder = AmazonS3ClientBuilder.standard();
      }

      if (awsRegion != null) {
         builder.withRegion(awsRegion);
      }
      return builder.build();
   }

   private static String getHash(MessageDigest digest) throws IOException {
      byte[] md5sum = digest.digest();
      BigInteger bigInt = new BigInteger(1, md5sum);
      String output = bigInt.toString(16);

      while (output.length() < 32) {
         output = "0" + output;
      }

      return output;
   }
}