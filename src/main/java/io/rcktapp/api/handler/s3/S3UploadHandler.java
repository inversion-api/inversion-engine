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

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Part;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.util.IOUtils;

import io.forty11.j.J;
import io.forty11.web.js.JSObject;
import io.rcktapp.api.Action;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Endpoint;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.SC;
import io.rcktapp.api.service.Service;

/**
 * Sends browser multi-part file uploads to a defined S3 location
 * 
 * Bean property config can be set directly on the handler in your
 * snooze.properties files but you should really consider this to be
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
public class S3UploadHandler implements Handler
{

   // mhuhman 4/3/2018
   // DO NOT USE the access key and secret key explicitly unless you are accessing another account's S3 bucket.
   // If snooze is running in the AWS account that owns the bucket being accessed, use IAM roles to authenticate!
   // Even if you're doing cross-account access, you can still use IAM authentication! 
   // see: https://docs.aws.amazon.com/IAM/latest/UserGuide/tutorial_cross-account-with-roles.html
   String accessKey       = null;
   String secretKey       = null;

   // region should remain null except when necessary for local testing
   String awsRegion       = null;

   // path will be built as such: staticBasePath/dynamicBasePath/requestPath/fileName
   // static and dynamic base paths set here are overridden by the .properties file
   // any basePath variables that are null will simply be skipped
   // dynamic base path MUST BE a date format - default is yyyy/MM/dd
   String bucket          = null;
   String staticBasePath  = null;
   String dynamicBasePath = "yyyy/MM/dd";

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {

      MessageDigest md = MessageDigest.getInstance("MD5");
      String requestPath = null;
      String fileName = null;
      Long fileSize = null;
      DigestInputStream imageInputStream = null;

      for (Part part : req.getHttpServletRequest().getParts())
      {
         if (part.getName() == null)
         {
            continue;
         }
         if (part.getName().equals("file"))
         {
            imageInputStream = new DigestInputStream(part.getInputStream(), md);
            String[] fileNameParts = part.getSubmittedFileName().split("[.]");
            fileName = "" + fileNameParts[0] + "-" + System.currentTimeMillis() + "." + fileNameParts[1];
            fileSize = part.getSize();
         }
         else if (part.getName().equals("requestPath"))
         {
            requestPath = IOUtils.toString(part.getInputStream());
            if (requestPath.indexOf("/") == 0)
               requestPath = requestPath.substring(1);
         }
      }

      if (imageInputStream == null)
      {
         error(res, null, "No file was uploaded in the multipart request");
         return;
      }

      //String pathAndFileName = buildFullPath(fileName, requestPath);
      Map<String, Object> responseContent = new HashMap<>();

      try
      {
         responseContent = saveFile(chain, imageInputStream, fileName, requestPath);
      }
      catch (Exception e)
      {
         error(res, e, "S3 Key may be invalid - valid characters are [  0-9 a-z A-Z !-_.*'()  ] --- your requested key was: " + requestPath + "/" + fileName);

         return;
      }

      responseContent.put("fileMd5", computeMd5Hash(imageInputStream, md));
      responseContent.put("fileSizeBytes", fileSize);
      res.setJson(new JSObject(responseContent));
   }

   void error(Response res, Exception exception, String message)
   {
      if (exception != null)
         message += "\r\n\r\n" + J.getShortCause(exception);

      res.setStatus(SC.SC_400_BAD_REQUEST);
      Map<String, String> content = new HashMap<>();
      content.put("message", message);
      content.put("error", "Bad Request Exception");
      res.setJson(new JSObject(content));

   }

   private Map<String, Object> saveFile(Chain chain, InputStream inputStream, String fileName, String requestPath) throws Exception
   {
      AmazonS3 s3 = buildS3Client(chain);
      String bucket = chain.getConfig("bucket", this.bucket);
      String pathAndFileName = buildFullPath(chain, requestPath, fileName);

      s3.putObject(new PutObjectRequest(bucket, pathAndFileName, inputStream, new ObjectMetadata()));

      Map<String, Object> resp = new HashMap<>();
      resp.put("url", "http://" + bucket + ".s3.amazonaws.com/" + pathAndFileName);
      resp.put("fileName", fileName);
      resp.put("path", pathAndFileName);

      return resp;
   }

   private String buildFullPath(Chain chain, String requestPath, String name)
   {
      StringBuilder sb = new StringBuilder();

      String staticBasePath = chain.getConfig("staticBasePath", this.staticBasePath);
      String dynamicBasePath = chain.getConfig("dynamicBasePath", this.dynamicBasePath);

      if (staticBasePath != null)
      {
         sb.append(staticBasePath);
         if (!staticBasePath.endsWith("/"))
         {
            sb.append("/");
         }
      }

      if (dynamicBasePath != null)
      {
         String datePath = new SimpleDateFormat(dynamicBasePath).format(new Date());
         sb.append(datePath + "/");
      }

      if (requestPath != null)
      {
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

   private AmazonS3 buildS3Client(Chain chain)
   {
      String accessKey = chain.getConfig("accessKey", this.accessKey);
      String secretKey = chain.getConfig("secretKey", this.secretKey);
      String awsRegion = chain.getConfig("awsRegion", this.awsRegion);

      AmazonS3ClientBuilder builder = null;
      if (accessKey != null)
      {
         BasicAWSCredentials creds = new BasicAWSCredentials(accessKey, secretKey);
         builder = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(creds));
      }
      else
      {
         builder = AmazonS3ClientBuilder.standard();
      }

      if (awsRegion != null)
      {
         builder.withRegion(awsRegion);
      }
      return builder.build();
   }

   private static String computeMd5Hash(DigestInputStream dis, MessageDigest digest) throws IOException
   {
      try
      {
         byte[] md5sum = digest.digest();
         BigInteger bigInt = new BigInteger(1, md5sum);
         String output = bigInt.toString(16);

         while (output.length() < 32)
         {
            output = "0" + output;
         }

         return output;
      }
      finally
      {
         dis.close();
      }
   }
}