/*
 * Copyright (c) 2015-2020 Rocket Partners, LLC
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
package io.inversion.cosmosdb;

import io.inversion.Db;
import io.inversion.rql.AbstractRqlTest;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class CosmosRqlUnitTest extends AbstractRqlTest {

    public CosmosRqlUnitTest() {
        super("northwind/cosmosdb/", "cosmosdb");

//        - likeMiddle - orders?like(shipCountry,F*ance) - 400 Bad Request - 400 Bad Request - The 'like' RQL operator for CosmosDb expects a single wildcard at the beginning OR the end of a value.  CosmosDb does not really support 'like' but compatible 'like' statements are turned into 'sw' or 'ew' statements that are supported.
//        - w - orders?w(shipCountry,ance) - 400 Bad Request - 400 Bad Request - CosmosDb supports 'sw' and 'ew' but not 'w' or 'wo' functions.
//                - wo - orders?wo(shipCountry,ance) - 400 Bad Request - 400 Bad Request - CosmosDb supports 'sw' and 'ew' but not 'w' or 'wo' functions.
        
        withExpectedResult("eq", "SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"orderId\\\"] = @orderId1 AND Northwind[\\\"shipCountry\\\"] = @shipCountry2 AND Northwind[\\\"type\\\"] = @type3 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@orderId1\",\"value\":10248},{\"name\":\"@shipCountry2\",\"value\":\"France\"},{\"name\":\"@type3\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("ne", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 AND (NOT (Northwind[\\\"shipCountry\\\"] = @shipCountry2)) ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"},{\"name\":\"@shipCountry2\",\"value\":\"France\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("n", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 AND IS_NULL (Northwind[\\\"shipRegion\\\"]) ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("nn", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 AND Northwind[\\\"shipRegion\\\"] <> null ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("emp", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE (Northwind[\\\"shipRegion\\\"] IS NULL OR Northwind[\\\"shipRegion\\\"] = '') AND Northwind[\\\"type\\\"] = @type1 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("nemp", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 AND (Northwind[\\\"shipRegion\\\"] IS NOT NULL AND Northwind[\\\"shipRegion\\\"] != '') ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("likeMiddle", "400 Bad Request - The 'like' RQL operator for CosmosDb expects a single wildcard at the beginning OR the end of a value.  CosmosDb does not really support 'like' but compatible 'like' statements are turned into 'sw' or 'ew' statements that are supported.");
//                                                             "400 Bad Request - 400 Bad Request - The 'like' RQL operator for CosmosDb expects a single wildcard at the beginning OR the end of a value.  CosmosDb does not really support 'like' but compatible 'like' statements are turned into 'sw' or 'ew' statements that are supported."
        
        withExpectedResult("likeStartsWith", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 AND STARTSWITH (Northwind[\\\"shipCountry\\\"], @shipCountry2) ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"},{\"name\":\"@shipCountry2\",\"value\":\"Franc\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("likeEndsWith", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 AND ENDSWITH (Northwind[\\\"shipCountry\\\"], @shipCountry2) ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"},{\"name\":\"@shipCountry2\",\"value\":\"ance\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("sw", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 AND STARTSWITH (Northwind[\\\"shipCountry\\\"], @shipCountry2) ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"},{\"name\":\"@shipCountry2\",\"value\":\"Franc\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("ew", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 AND ENDSWITH (Northwind[\\\"shipCountry\\\"], @shipCountry2) ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"},{\"name\":\"@shipCountry2\",\"value\":\"nce\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("w", "400 Bad Request - CosmosDb supports 'sw' and 'ew' but not 'w' or 'wo' functions.");
        withExpectedResult("wo", "400 Bad Request - CosmosDb supports 'sw' and 'ew' but not 'w' or 'wo' functions.");
        withExpectedResult("lt", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 AND Northwind[\\\"freight\\\"] < @freight2 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"},{\"name\":\"@freight2\",\"value\":10}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("le", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 AND Northwind[\\\"freight\\\"] <= @freight2 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"},{\"name\":\"@freight2\",\"value\":10}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("gt", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 AND Northwind[\\\"freight\\\"] > @freight2 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"},{\"name\":\"@freight2\",\"value\":3.67}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("ge", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 AND Northwind[\\\"freight\\\"] >= @freight2 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"},{\"name\":\"@freight2\",\"value\":3.67}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("in", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 AND Northwind[\\\"shipCity\\\"] IN(@shipCity2, @shipCity3) ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"},{\"name\":\"@shipCity2\",\"value\":\"Reims\"},{\"name\":\"@shipCity3\",\"value\":\"Charleroi\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("out", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 AND Northwind[\\\"shipCity\\\"] NOT IN(@shipCity2, @shipCity3) ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"},{\"name\":\"@shipCity2\",\"value\":\"Reims\"},{\"name\":\"@shipCity3\",\"value\":\"Charleroi\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("and", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"shipCity\\\"] = @shipCity1 AND Northwind[\\\"shipCountry\\\"] = @shipCountry2 AND Northwind[\\\"type\\\"] = @type3 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@shipCity1\",\"value\":\"Lyon\"},{\"name\":\"@shipCountry2\",\"value\":\"France\"},{\"name\":\"@type3\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("or", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 AND (Northwind[\\\"shipCity\\\"] = @shipCity2 OR Northwind[\\\"shipCity\\\"] = @shipCity3) ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"},{\"name\":\"@shipCity2\",\"value\":\"Reims\"},{\"name\":\"@shipCity3\",\"value\":\"Charleroi\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("not", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 AND NOT ((Northwind[\\\"shipCity\\\"] = @shipCity2 OR Northwind[\\\"shipCity\\\"] = @shipCity3)) ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"},{\"name\":\"@shipCity2\",\"value\":\"Reims\"},{\"name\":\"@shipCity3\",\"value\":\"Charleroi\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("as", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT *, Northwind[\\\"orderId\\\"] AS \\\"order_identifier\\\" FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("includes", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT Northwind[\\\"shipCountry\\\"], Northwind[\\\"shipCity\\\"], Northwind[\\\"type\\\"], Northwind[\\\"orderId\\\"] FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");


        withExpectedResult("distinct", "SqlQuerySpec={\"query\":\"SELECT DISTINCT Northwind[\\\"shipCountry\\\"], Northwind[\\\"type\\\"], Northwind[\\\"orderId\\\"] FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");

        withExpectedResult("count1", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT COUNT(*) FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("count2", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT COUNT(@null1) FROM Northwind WHERE Northwind[\\\"type\\\"] = @type2 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@null1\",\"value\":\"1\"},{\"name\":\"@type2\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("count3", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT COUNT(Northwind[\\\"shipRegion\\\"]) FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("countAs", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT COUNT(*) AS \\\"countOrders\\\" FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("sum", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT SUM(Northwind[\\\"freight\\\"]) FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("sumAs", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT SUM(Northwind[\\\"freight\\\"]) AS \\\"Sum Freight\\\" FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("sumIf", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT SUM(CASE WHEN Northwind[\\\"shipCountry\\\"] = @shipCountry1 THEN 1 ELSE 0 END) AS \\\"French Orders\\\" FROM Northwind WHERE Northwind[\\\"type\\\"] = @type2 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@shipCountry1\",\"value\":\"France\"},{\"name\":\"@type2\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("min", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT MIN(Northwind[\\\"freight\\\"]) FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("max", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT MAX(Northwind[\\\"freight\\\"]) FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("groupCount", "SqlQuerySpec={\"query\":\"SELECT Northwind[\\\"shipCountry\\\"], COUNT(*) AS \\\"countryCount\\\", Northwind[\\\"type\\\"], Northwind[\\\"orderId\\\"] FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 GROUP BY Northwind[\\\"shipCountry\\\"] ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("offset", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 3 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("limit", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 7\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("page", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 14 LIMIT 7\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("pageNum", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 14 LIMIT 7\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("after", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 ORDER BY Northwind[\\\"id\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("sort", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 ORDER BY Northwind[\\\"shipCountry\\\"] DESC, Northwind[\\\"shipCity\\\"] ASC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");
        withExpectedResult("order", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Northwind WHERE Northwind[\\\"type\\\"] = @type1 ORDER BY Northwind[\\\"shipCountry\\\"] ASC, Northwind[\\\"shipCity\\\"] DESC OFFSET 0 LIMIT 100\",\"parameters\":[{\"name\":\"@type1\",\"value\":\"orders\"}]} FeedOptions={enableCrossPartitionQuery=false}");

        withExpectedResult("onToManyExistsEq", "UNSUPPORTED");
        withExpectedResult("onToManyNotExistsNe", "UNSUPPORTED");
        withExpectedResult("manyToOneExistsEq", "UNSUPPORTED");
        withExpectedResult("manyToOneNotExistsNe", "UNSUPPORTED");
        withExpectedResult("manyTManyNotExistsNe", "UNSUPPORTED");
        withExpectedResult("eqNonexistantColumn", "UNSUPPORTED");
    }

    @Override
    public Db buildDb() {
        return CosmosDbFactory.buildDb();
    }

}
