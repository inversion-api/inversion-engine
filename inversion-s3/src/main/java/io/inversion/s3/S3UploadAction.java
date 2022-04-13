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

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import io.inversion.*;
import io.inversion.json.JSList;
import io.inversion.json.JSMap;
import io.inversion.utils.LimitInputStream;
import io.inversion.utils.Path;
import io.inversion.utils.Utils;
import org.apache.commons.io.input.CountingInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;

/**
 * Sends browser multi-part file uploads to a defined S3 location
 * <p>
 * Bean property config can be set directly on the handler in your
 * inversion.properties files but you should really consider this to be
 * a service singleton where the properties can be passed in via
 * Action config allowing a single handler instance to upload files
 * to multiple buckets based on request path.
 * <p>
 * So instead of config-ing something like
 * <p>
 * handler.dynamicBasePath=yyyy/MM/dd
 * or
 * handler.bucket=somebucket
 * <p>
 * do this
 * <p>
 * action.config=dynamicBasePath=yyyy/MM/dd@amp;bucket=somebucket
 * <p>
 * While accessKey/secreKey/awsRegion CAN be set either on the Handler
 * or on the Action in this way, if you control the host environment
 * and are uploading everyting to your own AWS account, you should
 * consider using IAM roles to authenticate.  Than way you don't need
 * to config the credentials at all.
 */
public class S3UploadAction extends Action<S3UploadAction> {

    protected String s3AccessKey = null;
    protected String s3SecretKey = null;
    protected String s3AwsRegion = null;

    protected String s3Bucket   = null;
    protected String s3BasePath = "uploads";
    protected String s3DatePath = "yyyy/MM/dd";

    protected String allowedCharactersRegex = "^[\\. \\(\\)\\'a-zA-Z0-9_-]*$";

    private static String getHash(MessageDigest digest) throws IOException {
        byte[]        md5sum = digest.digest();
        BigInteger    bigInt = new BigInteger(1, md5sum);
        StringBuilder output = new StringBuilder(bigInt.toString(16));

        while (output.length() < 32) {
            output.insert(0, "0");
        }

        return output.toString();
    }

    @Override
    public void run(Request req, Response res) throws ApiException {
        try {
            List<Upload> uploads = req.getUploads();

            boolean fileInUrl = req.getUrl().getParam("file") != null;

            if (uploads.size() > 1 && fileInUrl) {
                throw ApiException.new400BadRequest("You can not include a file name in your url if you post more than a single file.");
            }

            JSList arr = new JSList();

            for (Upload upload : uploads) {
                S3File file = saveFile(req, upload);
                JSMap  json = new JSMap();
                json.put("url", file.url);
                json.put("path", file.path);
                json.put("hash", file.hash);
                json.put("bytes", file.bytes);
                arr.add(json);
            }
            if (uploads.size() > 0) {
                if (fileInUrl) {
                    req.withJson(arr.getMap(0).copy());
                    res.withJson(arr.getMap(0).copy());
                } else {
                    req.withJson(arr.copy());
                    res.withJson(arr.copy());
                }
            }
        } catch (Exception ex) {
            throw ApiException.new500InternalServerError(ex);
        }
    }

    protected void onFileUploaded(Request req, Response resp, Upload upload, S3File file) {
        JSMap json = new JSMap();
        json.put("url", file.url);
        json.put("path", file.path);
        json.put("hash", file.hash);
        json.put("bytes", file.bytes);
        req.withJson(json.copy());
        resp.withJson(json.copy());
    }

    class S3File {
        String url;
        String path;
        String hash;
        long   bytes;
        String type;
    }

    private S3File saveFile(Request req, Upload upload) throws Exception {
        AmazonS3 s3     = buildS3Client();
        String   bucket = this.s3Bucket;
        String   path   = buildPath(req, upload);


        InputStream in = upload.getInputStream();
        if(upload.getFileSize() > 0)
            in = new LimitInputStream(in, upload.getFileSize(), true);

        CountingInputStream countIn  = new CountingInputStream(in);
        DigestInputStream   digestIn = new DigestInputStream(countIn, MessageDigest.getInstance("SHA-256"));
        PutObjectResult     s3Resp   = s3.putObject(new PutObjectRequest(bucket, path, digestIn, new ObjectMetadata()));

        String hash  = byteToHexString(digestIn.getMessageDigest().digest());
        long   bytes = countIn.getByteCount();

        S3File file = new S3File();
        file.url = "http://" + bucket + ".s3.amazonaws.com/" + path;
        file.path = path;
        file.hash = hash;
        file.bytes = bytes;
        return file;
    }

    public String byteToHexString(byte[] input) {
        String output = "";
        for (int i=0; i<input.length; ++i) {
            output += String.format("%02x", input[i]);
        }
        return output;
    }

    private String buildPath(Request request, Upload upload) {
        String urlFileName = request.getUrl().getParam("file");
        Path   path        = request.getPath().copy();

        if (urlFileName == null) {
            path.add(upload.getFileName());
        }

        if (!isValidPath(path))
            throw ApiException.new400BadRequest("The supplied file path is contains invalid characters");

        return path.toString();
    }

    protected boolean isValidPath(Path path) {
        if (path == null || path.size() == 0)
            return false;
        for (String part : path.parts()) {
            if (!part.matches(allowedCharactersRegex))
                return false;

            part = part.replaceAll("[.]+", ".");
            if (part.equals("."))
                return false;
        }
        return true;
    }

    private AmazonS3 buildS3Client() {
        //TODO make this work like dynamo client config as art of db

        String accessKey = this.s3AccessKey;
        String secretKey = this.s3SecretKey;
        String awsRegion = this.s3AwsRegion;

        AmazonS3ClientBuilder builder;
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
}