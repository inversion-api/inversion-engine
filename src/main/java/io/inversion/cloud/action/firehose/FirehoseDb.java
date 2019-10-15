/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * https://github.com/inversion-api
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
package io.inversion.cloud.action.firehose;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClientBuilder;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.Record;

import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Attribute;
import io.inversion.cloud.model.Db;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Results;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.model.Table;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.utils.Rows.Row;
import io.inversion.cloud.utils.Utils;

/**
 * Posts records to a mapped AWS Kinesis Firehose stream. 
 * 
 * When you PUT/POST a:
 * <ul>
 * <li>a JSON object - it is submitted as a single record
 * <li>a JSON array - each element in the array is submitted as a single record.
 * </ul>
 * 
 * Unless <code>jsonPrettyPrint</code> is set to <code>true</code> all JSON
 * records are stringified without return characters.
 * 
 * All records are always submitted in batches of up to <code>batchMax</code>.  
 * You can submit more than <code>batchMax</code> to the handler and it will try to
 * send as many batches as required. 
 * 
 * If <code>jsonSeparator</code> is not null (it is '\n' by default) and the 
 * stringified record does not end in <code>separator</code>,
 * <code>separator</code> will be appended to the record.
 * 
 * If your firehose is Redshift, you probably want to leave <code>jsonLowercaseNames</code>
 * at its default which is true.  Redshift only matches to lowercase names on COPY.
 * 
 * The underlying Firehose stream is mapped to the collection name through
 * the FireshoseDb.includeStreams property.
 * 
 * 
 * @author wells
 *
 */
public class FirehoseDb extends Db<FirehoseDb>
{
   /**
    * A CSV of pipe delimited collection name to table name pairs.
    * 
    * Example: firehosedb.includeStreams=impression|liftck-player9-impression
    * 
    * Or if the collection name is the name as the table name you can just send a the name
    * 
    * Example: firehosedb.includeStreams=liftck-player9-impression
    */
   protected String                includeStreams     = null;

   protected String                awsAccessKey       = null;
   protected String                awsSecretKey       = null;
   protected String                awsRegion          = null;

   protected AmazonKinesisFirehose firehoseClient     = null;

   protected int                   batchMax           = 500;
   protected String                jsonSeparator      = "\n";
   protected boolean               jsonPrettyPrint    = false;
   protected boolean               jsonLowercaseNames = true;

   public FirehoseDb()
   {
      this.withType("firehose");
   }

   @Override
   protected void startup0()
   {
      if (!Utils.empty(includeStreams))
      {
         for (String part : Utils.explode(",", includeStreams))
         {
            String[] arr = part.split("\\|");
            String collectionName = arr[0];
            String streamName = collectionName;
            if (arr.length > 1)
            {
               streamName = arr[1];
            }

            Table table = new Table(this, streamName);
            withTable(table);

            if (arr.length == 1)//a specific collection name was not supplied by the config
               collectionName = beautifyCollectionName(collectionName);

            api.makeCollection(table, collectionName);
         }
      }
      else
      {
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "FirehoseDb must have 'includeStreams' configured to be used");
      }
   }

   @Override
   public Results<Row> select(Table table, List<Term> columnMappedTerms) throws Exception
   {
      throw new ApiException(SC.SC_400_BAD_REQUEST, "The Firehose handler only supports PUT/POST operations...GET and DELETE don't make sense.");
   }

   @Override
   public void delete(Table table, String entityKey) throws Exception
   {
      throw new ApiException(SC.SC_400_BAD_REQUEST, "The Firehose handler only supports PUT/POST operations...GET and DELETE don't make sense.");
   }

   @Override
   public String upsert(Table table, Map<String, Object> row) throws Exception
   {
      List<String> keys = upsert(table, Arrays.asList(row));
      if (keys != null && keys.size() > 0)
         return keys.get(0);

      return null;
   }

   @Override
   public List upsert(Table table, List<Map<String, Object>> rows) throws Exception
   {
      List<Record> batch = new ArrayList();
      for (int i = 0; i < rows.size(); i++)
      {
         String string = new JSNode(rows.get(i)).toString(jsonPrettyPrint, jsonLowercaseNames);

         if (jsonSeparator != null && !string.endsWith(jsonSeparator))
            string += jsonSeparator;

         batch.add(new Record().withData(ByteBuffer.wrap(string.getBytes())));

         if (i > 0 && i % batchMax == 0)
         {
            getFirehoseClient().putRecordBatch(new PutRecordBatchRequest().withDeliveryStreamName(table.getName()).withRecords(batch));
            batch.clear();
         }
      }

      if (batch.size() > 0)
      {
         getFirehoseClient().putRecordBatch(new PutRecordBatchRequest().withDeliveryStreamName(table.getName()).withRecords(batch));
      }

      return Collections.emptyList();
   }

   public AmazonKinesisFirehose getFirehoseClient()
   {
      return getFirehoseClient(awsRegion, awsAccessKey, awsSecretKey);
   }

   public AmazonKinesisFirehose getFirehoseClient(String awsRegion, String awsAccessKey, String awsSecretKey)
   {
      if (this.firehoseClient == null)
      {
         synchronized (this)
         {
            if (this.firehoseClient == null)
            {
               awsRegion = Utils.findSysEnvPropStr(getName() + ".awsRegion", awsRegion);
               awsAccessKey = Utils.findSysEnvPropStr(getName() + ".awsAccessKey", awsAccessKey);
               awsSecretKey = Utils.findSysEnvPropStr(getName() + ".awsSecretKey", awsSecretKey);

               AmazonKinesisFirehoseClientBuilder builder = AmazonKinesisFirehoseClientBuilder.standard();

               if (!Utils.empty(awsRegion))
                  builder.withRegion(awsRegion);

               if (!Utils.empty(awsAccessKey) && !Utils.empty(awsSecretKey))
               {
                  BasicAWSCredentials creds = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
                  builder.withCredentials(new AWSStaticCredentialsProvider(creds));
               }

               firehoseClient = builder.build();
            }
         }
      }

      return firehoseClient;
   }

   public FirehoseDb withAwsRegion(String awsRegion)
   {
      this.awsRegion = awsRegion;
      return this;
   }

   public FirehoseDb withAwsAccessKey(String awsAccessKey)
   {
      this.awsAccessKey = awsAccessKey;
      return this;
   }

   public FirehoseDb withAwsSecretKey(String awsSecretKey)
   {
      this.awsSecretKey = awsSecretKey;
      return this;
   }

   public FirehoseDb withIncludeStreams(String includeStreams)
   {
      this.includeStreams = includeStreams;
      return this;
   }

   public FirehoseDb withBatchMax(int batchMax)
   {
      this.batchMax = batchMax;
      return this;
   }

   public FirehoseDb withJsonSeparator(String jsonSeparator)
   {
      this.jsonSeparator = jsonSeparator;
      return this;
   }

   public FirehoseDb withJsonPrettyPrint(boolean jsonPrettyPrint)
   {
      this.jsonPrettyPrint = jsonPrettyPrint;
      return this;
   }

   public FirehoseDb withJsonLowercaseNames(boolean jsonLowercaseNames)
   {
      this.jsonLowercaseNames = jsonLowercaseNames;
      return this;
   }
}
