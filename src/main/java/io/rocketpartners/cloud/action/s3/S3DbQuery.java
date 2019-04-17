package io.rocketpartners.cloud.action.s3;

import java.util.List;

import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import io.rocketpartners.cloud.model.Results;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.rql.Group;
import io.rocketpartners.cloud.rql.Order;
import io.rocketpartners.cloud.rql.Page;
import io.rocketpartners.cloud.rql.Query;
import io.rocketpartners.cloud.rql.Select;
import io.rocketpartners.cloud.rql.Term;
import io.rocketpartners.cloud.rql.Where;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.utils.Rows.Row;

/**
 * @author tc-rocket, wells
 * 
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
public class S3DbQuery extends Query<S3DbQuery, S3Db, Table, Select<Select<Select, S3DbQuery>, S3DbQuery>, Where<Where<Where, S3DbQuery>, S3DbQuery>, Group<Group<Group, S3DbQuery>, S3DbQuery>, Order<Order<Order, S3DbQuery>, S3DbQuery>, Page<Page<Page, S3DbQuery>, S3DbQuery>>
{
   public S3DbQuery(Table table, List<Term> terms)
   {
      super(table, terms);
      where().clearFunctions();
      where().withFunctions("eq", "sw");
   }

   protected Results<Row> doSelect() throws Exception
   {
      // path == /s3/bucketName
      // path == /s3/bucketName/inner/folder
      // retrieve as much meta data as possible about the files in the bucket

      ListObjectsRequest req = new ListObjectsRequest();
      req.setBucketName(table().getName());
      req.setMaxKeys(page().getLimit()); // TODO fix pagesize...currently always set to 1000 ... tied to 'size' but not 'pagesize'?
      req.setDelimiter("/");

      String prefix = Chain.getRequest().getSubpath();

      while (prefix.startsWith("/"))
         prefix = prefix.substring(1, prefix.length());

      while (prefix.endsWith("/"))
         prefix = prefix.substring(0, prefix.length() - 1);

      req.setPrefix(prefix);

      ObjectListing listing = getDb().getS3Client().listObjects(req);

      Results results = new Results(this);

      if (listing.isTruncated())
      {
         results.withNext(Term.term(null, "after", listing.getNextMarker()));
      }

      List<String> directoryList = listing.getCommonPrefixes();
      List<S3ObjectSummary> fileList = listing.getObjectSummaries();

      // alphabetize the data returned to the client...
      while (!directoryList.isEmpty())
      {
         String directory = directoryList.get(0);
         if (!fileList.isEmpty())
         {
            S3ObjectSummary file = fileList.get(0);
            if (directory.compareToIgnoreCase(file.getKey()) < 0)
            {
               // directory name comes before file name
               //results.withRow(buildListObj(req.getApiUrl() + req.getPath() + directory, null, null, false));
               directoryList.remove(0);
            }
            else
            {
               // file name comes before directory
               //results.withRow(buildListObj(req.getApiUrl() + req.getPath() + file.getKey(), file.getLastModified(), file.getSize(), true));
               fileList.remove(0);
            }
         }
         else
         {
            //results.withRow(buildListObj(req.getApiUrl() + req.getPath() + directory, null, null, false));
            directoryList.remove(0);
         }
      }

      while (!fileList.isEmpty())
      {
         S3ObjectSummary file = fileList.remove(0);
         //results.withRow(buildListObj(req.getApiUrl() + req.getPath() + file.getKey(), file.getLastModified(), file.getSize(), true));
      }

      return results;
   }

}
