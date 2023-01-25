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

import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import io.inversion.*;
import io.inversion.rql.*;
import io.inversion.utils.Path;
import io.inversion.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_Query.html
 * https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/index.html
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Introduction.html
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/QueryingJavaDocumentAPI.html
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Query.html#FilteringResults
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.ExpressionAttributeNames.html
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.KeyConditions.html
 */
public class S3DbQuery extends Query<S3DbQuery, S3Db, Select<Select<Select, S3DbQuery>, S3DbQuery>, From<From<From, S3DbQuery>, S3DbQuery>, Where<Where<Where, S3DbQuery>, S3DbQuery>, Group<Group<Group, S3DbQuery>, S3DbQuery>, Order<Order<Order, S3DbQuery>, S3DbQuery>, Page<Page<Page, S3DbQuery>, S3DbQuery>> {

    Path securePrefix = null;

    public S3DbQuery(S3Db db, Collection table, Path securePrefix, List<Term> terms) {
        super(db, table, terms);
        this.securePrefix = securePrefix;
        getWhere().clearFunctions();
        getWhere().withFunctions("eq", "sw");
    }

    public Results doSelect() throws ApiException {
        // path == /s3/bucketName
        // path == /s3/bucketName/inner/folder
        // retrieve as much meta data as possible about the files in the bucket

        Path prefix = new Path();
        if (securePrefix != null) {
            prefix.add(securePrefix.toString());
        }

        //-- add the url path to the prefix, removing collection/bucket identifier
        Request req        = Chain.peek().getRequest();
        Path    pathPrefix = req.getPath();
        Path    epPath     = req.getEndpointPath();
        pathPrefix = pathPrefix.subpath(epPath.size(), pathPrefix.size());
        if (pathPrefix.size() >= 1)
            pathPrefix = pathPrefix.subpath(1, pathPrefix.size());
        prefix.add(pathPrefix.toString());
        //-- end url pathing


        ListObjectsRequest s3Req = new ListObjectsRequest();
        s3Req.setBucketName(getCollection().getTableName());
        s3Req.setMaxKeys(getPage().getLimit());
        if (prefix.size() > 0) {
            s3Req.setPrefix(prefix.toString() + "/");
        }
        s3Req.setDelimiter("/");

        System.out.println("prefix    = " + s3Req.getPrefix());
        System.out.println("delimiter = " + s3Req.getDelimiter());


        ObjectListing listing = getDb().getS3Client().listObjects(s3Req);

        Results results = new Results(this);

        if (listing.isTruncated()) {
            results.withNext(Term.term(null, "after", listing.getNextMarker()));
        }

        List<String>          directoryList = listing.getCommonPrefixes();
        List<S3ObjectSummary> fileList      = listing.getObjectSummaries();

        List found = new ArrayList();

        // alphabetize the data returned to the client...
        while (!directoryList.isEmpty()) {
            String directory = directoryList.remove(0);
            found.add(directory);

//            if (!fileList.isEmpty()) {
//                S3ObjectSummary file = fileList.get(0);
//                if (directory.compareToIgnoreCase(file.getKey()) < 0) {
//                    // directory name comes before file name
//                    //results.withRow(buildListObj(req.getApiUrl() + req.getPath() + directory, null, null, false));
//                    directoryList.remove(0);
//                } else {
//                    // file name comes before directory
//                    //results.withRow(buildListObj(req.getApiUrl() + req.getPath() + file.getKey(), file.getLastModified(), file.getSize(), true));
//                    fileList.remove(0);
//                }
//            } else {
//                //results.withRow(buildListObj(req.getApiUrl() + req.getPath() + directory, null, null, false));
//
//                String dir = directoryList.remove(0);
//                Map row = Utils.asMap("key", dir);
//            }
        }

        while (!fileList.isEmpty()) {
            S3ObjectSummary file = fileList.remove(0);
            found.add(file);
        }

        String removeSecure = null;
        if (securePrefix != null)
            removeSecure = securePrefix.toString() + "/";


        for (Object obj : found) {

            String key = obj instanceof String ? (String) obj : ((S3ObjectSummary) obj).getKey();

            if (removeSecure != null)
                key = key.substring(removeSecure.length());

            Request root = Chain.peek().getRequest();
            Url     url  = root.getUrl().copy();
            url.clearParams();

            Path path = new Path(root.getServerPath());
            path.add(root.getEndpointPath().toString());
            path.add(root.getActionPath().first());
            path.add(key);
            url.withPath(path);

            Map row = Utils.asMap("key", key, "href", url.toString());
            results.withRow(row);
        }
        return results;
    }


}
