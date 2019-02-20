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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.Record;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.ArrayNode;
import io.rocketpartners.cloud.model.Attribute;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.ObjectNode;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Service;

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
public class FirehosePostAction extends Action<FirehosePostAction>
{
   protected int     batchMax           = 500;
   protected String  jsonSeparator      = "\n";
   protected boolean jsonPrettyPrint    = false;
   protected boolean jsonLowercaseNames = true;

   @Override
   public void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      if (!req.isMethod("PUT", "POST"))
         throw new ApiException(SC.SC_400_BAD_REQUEST, "The Firehose handler only supports PUT/POST operations...GET and DELETE don't make sense.");

      ObjectNode body = req.getJson();

      if (body == null)
         throw new ApiException(SC.SC_400_BAD_REQUEST, "Attempting to post an empty body to a Firehose stream");

      if (!(body instanceof ArrayNode))
         body = new ArrayNode(body);

      ArrayNode array = (ArrayNode) body;

      Collection collection = req.getCollection();
      Table table = collection.getTable();

      String streamName = table.getName();
      AmazonKinesisFirehose firehose = ((FirehoseDb) table.getDb()).getFirehoseClient();

      List<Record> batch = new ArrayList();
      for (int i = 0; i < array.length(); i++)
      {
         Object data = array.get(i);

         if (data instanceof ObjectNode)
         {
            //remap from attribute to column names if this db has been configured with them
            //this db/action can be used without collection attribute definitions and the 
            //json will just be thrown into the stream as is

            List<Attribute> attrs = collection.getEntity().getAttributes();
            if (attrs.size() > 0)
            {
               ObjectNode newJson = new ObjectNode();
               ObjectNode oldJson = (ObjectNode) data;

               for (Attribute attr : attrs)
               {
                  //reorder the properties with their column names
                  Object value = oldJson.remove(attr.getName());
                  newJson.put(attr.getColumn().getName(), value);
               }
               for (String key : oldJson.keySet())
               {
                  //add any remaining unmapped attributes
                  newJson.put(key, oldJson.get(key));
               }
               data = newJson;
            }
         }

         if (data == null)
            continue;

         String string = data instanceof ObjectNode ? ((ObjectNode) data).toString(jsonPrettyPrint, jsonLowercaseNames) : data.toString();

         if (jsonSeparator != null && !string.endsWith(jsonSeparator))
            string += jsonSeparator;

         batch.add(new Record().withData(ByteBuffer.wrap(string.getBytes())));

         if (i > 0 && i % batchMax == 0)
         {
            firehose.putRecordBatch(new PutRecordBatchRequest().withDeliveryStreamName(streamName).withRecords(batch));
            batch.clear();
         }
      }

      if (batch.size() > 0)
      {
         firehose.putRecordBatch(new PutRecordBatchRequest().withDeliveryStreamName(streamName).withRecords(batch));
      }

      res.withStatus(SC.SC_201_CREATED);
   }

   public FirehosePostAction withBatchMax(int batchMax)
   {
      this.batchMax = batchMax;
      return this;
   }

   public FirehosePostAction withJsonSeparator(String jsonSeparator)
   {
      this.jsonSeparator = jsonSeparator;
      return this;
   }

   public FirehosePostAction withJsonPrettyPrint(boolean jsonPrettyPrint)
   {
      this.jsonPrettyPrint = jsonPrettyPrint;
      return this;
   }

   public FirehosePostAction withJsonLowercaseNames(boolean jsonLowercaseNames)
   {
      this.jsonLowercaseNames = jsonLowercaseNames;
      return this;
   }

}
