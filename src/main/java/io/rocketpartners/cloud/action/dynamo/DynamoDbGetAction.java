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
package io.rocketpartners.cloud.action.dynamo;

import io.rocketpartners.cloud.action.dynamo.DynamoDb.DynamoDbIndex;
import io.rocketpartners.cloud.action.dynamo.DynamoDbQuery.DynamoResult;
import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Endpoint;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.Response;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Service;

public class DynamoDbGetAction extends DynamoDbAction
{
   @Override
   public void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      Collection collection = req.getCollection();
      Table table = collection.getTable();
      DynamoDb db = (DynamoDb) table.getDb();
      com.amazonaws.services.dynamodbv2.document.Table dynamoTable = db.getDynamoTable(table.getName());

      DynamoDbQuery query = new DynamoDbQuery(collection, req.getParams());

      String entityKey = req.getEntityKey();
      if (entityKey != null)
      {
         String[] parts = DynamoDb.fromEntityKey(entityKey);
         String hashKey = parts[0];
         String sortKey = parts[1];
         
         DynamoDbIndex index = (DynamoDbIndex)table.getIndex(DynamoDbIndex.PRIMARY_INDEX);
         if(index != null)
         {
            String hashAttrName = collection.getAttributeName(index.getHashKey().getName());
            query.withTerm("eq(" + hashAttrName + "," + hashKey + ")");
            
            if(sortKey != null)
            {
               String sortAttrName = collection.getAttributeName(index.getSortKey().getName());
               query.withTerm("eq(" + sortAttrName + "," + sortKey + ")");
            }
         }
      }

      DynamoResult dynamoResult = query.doSelect(dynamoTable);

      res.withPageSize(query.page().getPageSize())//
         .withPageNum(query.page().getPageNum());

      for (Object js : dynamoResult.rows)
      {
         res.withRecord(js);
      }
   }
}
