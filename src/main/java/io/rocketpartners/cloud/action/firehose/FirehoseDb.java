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
package io.rocketpartners.cloud.action.firehose;

import java.util.List;
import java.util.Map;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClientBuilder;

import io.rocketpartners.cloud.model.Db;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Results;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.rql.Query;
import io.rocketpartners.cloud.rql.Term;
import io.rocketpartners.cloud.utils.Utils;

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
   protected String                includeStreams = null;

   protected String                awsAccessKey   = null;
   protected String                awsSecretKey   = null;
   protected String                awsRegion      = null;

   protected AmazonKinesisFirehose firehoseClient = null;

   public FirehoseDb()
   {
      this.withType("firehose");
   }

   public Results<Map<String, Object>> select(Request request, Table table, List<Term> columnMappedTerms) throws Exception
   {
      return null;
   }

   public String upsert(Request request, Table table, Map<String, Object> values) throws Exception
   {
      return null;
   }

   public void delete(Request request, Table table, String entityKey) throws Exception
   {

   }

   @Override
   public void bootstrapApi()
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

            api.withCollection(table, collectionName);
         }
      }
      else
      {
         log.warn("FirehoseDb must have 'includeStreams' configured to be used");
      }
   }

   public AmazonKinesisFirehose getFirehoseClient()
   {
      if (this.firehoseClient == null)
      {
         synchronized (this)
         {
            if (this.firehoseClient == null)
            {
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

}
