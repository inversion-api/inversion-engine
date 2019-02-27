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

import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.document.DeleteItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;

import io.rocketpartners.cloud.action.dynamo.DynamoDb.DynamoDbIndex;
import io.rocketpartners.cloud.action.rest.RestDeleteAction;
import io.rocketpartners.cloud.model.ApiException;
import io.rocketpartners.cloud.model.Collection;
import io.rocketpartners.cloud.model.Request;
import io.rocketpartners.cloud.model.SC;
import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.rql.Term;
import io.rocketpartners.cloud.utils.Utils;

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

      List<Term> terms = collection.getEntity().decodeKey(entityKey);

      Object hashKeyValue = terms.get(0).getToken(1);
      Object sortKeyValue = terms.size() > 1 ? terms.get(1).getToken(1) : null;

      String hashKeyName = dynamoIdx.getHashKeyName();
      String sortKeyName = dynamoIdx.getSortKeyName();

      hashKeyValue = db.cast(table.getColumn(hashKeyName).getType(), hashKeyValue);
      sortKeyValue = sortKeyValue == null || sortKeyName == null ? null : db.cast(table.getColumn(sortKeyName).getType(), sortKeyValue);

      if (sortKeyName != null && Utils.empty(sortKeyValue) || sortKeyName == null && !Utils.empty(sortKeyValue))
         throw new ApiException(SC.SC_400_BAD_REQUEST, "The request usage of sort key does not match the table spec");

      String tableName = table.getName();
      com.amazonaws.services.dynamodbv2.document.Table dynamoTable = db.getDynamoTable(tableName);

      if (sortKeyName != null)
      {
         dynamoTable.deleteItem(hashKeyName, hashKeyValue, sortKeyName, sortKeyValue);
         Item item = dynamoTable.getItem(hashKeyName, hashKeyValue, sortKeyName, sortKeyValue);
         System.out.println(item);
      }
      else
      {
         dynamoTable.deleteItem(hashKeyName, hashKeyValue);
      }

      //      Item deleted = result.getItem();
      //      if (deleted == null)
      //         throw new ApiException(SC.SC_404_NOT_FOUND, "Entity '" + entityKey + "' was not deleted because it was not found");
   }
}
