package io.rcktapp.api.handler.firehose;

import org.atteo.evo.inflector.English;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClientBuilder;

import io.forty11.j.J;
import io.rcktapp.api.Collection;
import io.rcktapp.api.Db;
import io.rcktapp.api.Entity;
import io.rcktapp.api.Table;

public class FirehoseDb extends Db
{
   protected String      awsRegion      = null;

   /**
    * A CSV of pipe delimited collection name to table name pairs.
    * 
    * Example: firehosedb.includeStreams=impression|liftck-player9-impression
    * 
    * Or if the collection name is the name as the table name you can just send a the name
    * 
    * Example: firehosedb.includeStreams=liftck-player9-impression
    */
   protected String      includeStreams;

   AmazonKinesisFirehose firehoseClient = null;

   @Override
   public void bootstrapApi() throws Exception
   {
      AmazonKinesisFirehose firehoseClient = getFirehoseClient();

      this.setType("firehose");

      if (!J.empty(includeStreams))
      {
         String[] parts = includeStreams.split(",");
         for (String part : parts)
         {
            String[] arr = part.split("\\|");
            String collectionName = arr[0];
            String streamName = collectionName;
            if (arr.length > 1)
            {
               streamName = arr[1];
            }

            Table table = new Table(this, streamName);
            addTable(table);

            Collection collection = new Collection();
            if (!collectionName.endsWith("s"))
               collectionName = English.plural(collectionName);

            collection.setName(collectionName);

            Entity entity = new Entity();
            entity.setTbl(table);
            entity.setHint(table.getName());
            entity.setCollection(collection);

            collection.setEntity(entity);

            api.addCollection(collection);
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
         if (J.empty(awsRegion))
         {
            firehoseClient = AmazonKinesisFirehoseClientBuilder.defaultClient();
         }
         else
         {
            firehoseClient = AmazonKinesisFirehoseClientBuilder.standard().withRegion(awsRegion).build();
         }

      }

      return firehoseClient;
   }

}
