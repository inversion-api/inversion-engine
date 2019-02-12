package io.rocketpartners.cloud.handler.firehose;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.Record;

import io.rocketpartners.cloud.model.Action;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Handler;
import io.rocketpartners.cloud.service.Request;
import io.rocketpartners.cloud.service.Response;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.utils.JSArray;
import io.rocketpartners.utils.JSObject;

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
public class FirehosePostHandler implements Handler
{
   protected int     batchMax           = 500;
   protected String  jsonSeparator      = "\n";
   protected boolean jsonPrettyPrint    = false;
   protected boolean jsonLowercaseNames = true;

   @Override
   public void service(Service service, Api api, Endpoint endpoint, Action action, Chain chain, Request req, Response res) throws Exception
   {
      if (!req.isMethod("PUT", "POST"))
         throw new ApiException(SC.SC_400_BAD_REQUEST, "The Firehose handler only supports PUT/POST operations...GET and DELETE don't make sense.");

      String collectionKey = req.getCollectionKey();
      Collection col = api.getCollection(collectionKey, FirehoseDb.class);
      Table table = col.getEntity().getTable();
      String streamName = table.getName();

      AmazonKinesisFirehose firehose = ((FirehoseDb) table.getDb()).getFirehoseClient();

      JSObject body = req.getJson();

      if (body == null)
         throw new ApiException(SC.SC_400_BAD_REQUEST, "Attempting to post an empty body to a Firehose stream");

      if (!(body instanceof JSArray))
         body = new JSArray(body);

      JSArray array = (JSArray) body;

      List<Record> batch = new ArrayList();

      for (int i = 0; i < array.length(); i++)
      {
         Object data = array.get(i);

         if (data == null)
            continue;

         String string = data instanceof JSObject ? ((JSObject) data).toString(jsonPrettyPrint, jsonLowercaseNames) : data.toString();

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

      res.setStatus(SC.SC_201_CREATED);
   }
}
