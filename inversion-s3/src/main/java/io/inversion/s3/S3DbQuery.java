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

import java.util.List;

import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import io.inversion.ApiException;
import io.inversion.Chain;
import io.inversion.Collection;
import io.inversion.Results;
import io.inversion.rql.From;
import io.inversion.rql.Group;
import io.inversion.rql.Order;
import io.inversion.rql.Page;
import io.inversion.rql.Query;
import io.inversion.rql.Select;
import io.inversion.rql.Term;
import io.inversion.rql.Where;

/**
 * @see https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_Query.html
 * 
 * @see https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/index.html
 * @see https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Introduction.html
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/QueryingJavaDocumentAPI.html
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Query.html#FilteringResults
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.ExpressionAttributeNames.html
 * 
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/LegacyConditionalParameters.KeyConditions.html
 * 
 * 
 */
public class S3DbQuery extends Query<S3DbQuery, S3Db, Select<Select<Select, S3DbQuery>, S3DbQuery>, From<From<From, S3DbQuery>, S3DbQuery>, Where<Where<Where, S3DbQuery>, S3DbQuery>, Group<Group<Group, S3DbQuery>, S3DbQuery>, Order<Order<Order, S3DbQuery>, S3DbQuery>, Page<Page<Page, S3DbQuery>, S3DbQuery>> {

   public S3DbQuery(S3Db db, Collection table, List<Term> terms) {
      super(db, table, terms);
      getWhere().clearFunctions();
      getWhere().withFunctions("eq", "sw");
   }

   public Results doSelect() throws ApiException {
      // path == /s3/bucketName
      // path == /s3/bucketName/inner/folder
      // retrieve as much meta data as possible about the files in the bucket

      ListObjectsRequest req = new ListObjectsRequest();
      req.setBucketName(getCollection().getTableName());
      req.setMaxKeys(getPage().getLimit()); // TODO fix pagesize...currently always set to 1000 ... tied to 'size' but not 'pagesize'?
      req.setDelimiter("/");

      String prefix = Chain.peek().getRequest().getSubpath().toString();

      while (prefix.startsWith("/"))
         prefix = prefix.substring(1, prefix.length());

      while (prefix.endsWith("/"))
         prefix = prefix.substring(0, prefix.length() - 1);

      req.setPrefix(prefix);

      ObjectListing listing = getDb().getS3Client().listObjects(req);

      Results results = new Results(this);

      if (listing.isTruncated()) {
         results.withNext(Term.term(null, "after", listing.getNextMarker()));
      }

      List<String> directoryList = listing.getCommonPrefixes();
      List<S3ObjectSummary> fileList = listing.getObjectSummaries();

      // alphabetize the data returned to the client...
      while (!directoryList.isEmpty()) {
         String directory = directoryList.get(0);
         if (!fileList.isEmpty()) {
            S3ObjectSummary file = fileList.get(0);
            if (directory.compareToIgnoreCase(file.getKey()) < 0) {
               // directory name comes before file name
               //results.withRow(buildListObj(req.getApiUrl() + req.getPath() + directory, null, null, false));
               directoryList.remove(0);
            }
            else {
               // file name comes before directory
               //results.withRow(buildListObj(req.getApiUrl() + req.getPath() + file.getKey(), file.getLastModified(), file.getSize(), true));
               fileList.remove(0);
            }
         }
         else {
            //results.withRow(buildListObj(req.getApiUrl() + req.getPath() + directory, null, null, false));
            directoryList.remove(0);
         }
      }

      while (!fileList.isEmpty()) {
         S3ObjectSummary file = fileList.remove(0);
         //results.withRow(buildListObj(req.getApiUrl() + req.getPath() + file.getKey(), file.getLastModified(), file.getSize(), true));
      }

      return results;
   }

}
