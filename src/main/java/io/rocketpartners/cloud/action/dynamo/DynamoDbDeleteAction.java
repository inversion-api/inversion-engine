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

import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;

import io.rocketpartners.cloud.action.dynamo.DynamoDb.DynamoDbIndex;
import io.rocketpartners.cloud.action.rest.RestDeleteAction;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.Table;

/**
 * @author tc-rocket
 *
 */
public class DynamoDbDeleteAction extends RestDeleteAction
{

   @Override
   protected void deleteEntity(Request request, Collection collection, String entityKey) throws Exception
   {
      Table table = collection.getTable();
      DynamoDb db = (DynamoDb) table.getDb();
      
      DynamoDbIndex dynamoIdx = DynamoDb.findIndexByName(table, DynamoDbIndex.PRIMARY_INDEX);

      String[] keys = DynamoDb.fromEntityKey(entityKey);

      Object hashKey = keys[0];
      Object sortKey = keys[1];

      hashKey = DynamoDb.cast(hashKey, table.getColumn(dynamoIdx.getHashKeyName()).getType());
      sortKey = sortKey == null || dynamoIdx.getSortKeyName() == null ? null : DynamoDb.cast((String) sortKey, table.getColumn(dynamoIdx.getSortKeyName()).getType());

      String hashKeyName = dynamoIdx.getHashKeyName();
      String sortKeyName = dynamoIdx.getSortKeyName();

      if (sortKey != null && keys[1] == null || sortKey == null && keys[1] != null)
         throw new ApiException(SC.SC_400_BAD_REQUEST, "The request usage of sort key does not match the table spec");

      DeleteItemSpec spec = new DeleteItemSpec();
      if (sortKeyName != null)
      {
         spec.withPrimaryKey(hashKeyName, hashKey, sortKeyName, sortKey);
      }
      else
      {
         spec.withPrimaryKey(hashKeyName, hashKey);
      }
      
      com.amazonaws.services.dynamodbv2.document.Table dynamoTable = db.getDynamoTable(table.getName());
      dynamoTable.deleteItem(spec);
   }
}
