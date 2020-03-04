/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.model;

import io.inversion.cloud.service.MockDb;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DbTest
{
    public void testAttributeBeautification()
    {
        Db db = new MockDb()
        {
        };

        assertEquals("somecolumn", db.beautifyAttributeName("SOMECOLUMN"));
        assertEquals("someColumn", db.beautifyAttributeName("SomeColumn"));
        assertEquals("sOMEcolumn", db.beautifyAttributeName("SOMEcolumn"));
      assertEquals("someColumn",  db.beautifyAttributeName("SOME_COLUMN"));
      assertEquals("someColumn",  db.beautifyAttributeName("_SOME_COLUMN_"));
      assertEquals("someColumn",  db.beautifyAttributeName("_SOME_  ____COLUMN _ "));
      assertEquals("someColumn",  db.beautifyAttributeName("_some_column_"));
      
      assertEquals("someColumn",  db.beautifyAttributeName(" SOME COLUMN "));
      assertEquals("someColumn",  db.beautifyAttributeName("    SOME    COLUMN   "));
      assertEquals("someColumn",  db.beautifyAttributeName(" some column "));
      assertEquals("someColumn",  db.beautifyAttributeName("        some   column   "));
      
      assertEquals("x23SomeColumn",  db.beautifyAttributeName("123        some   column   "));
      assertEquals("$someColumn",  db.beautifyAttributeName("$some_column"));
      assertEquals("orderDetails",  db.beautifyAttributeName("Order Details"));
      
      
   }
}
