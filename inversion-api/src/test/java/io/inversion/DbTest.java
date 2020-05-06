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
package io.inversion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.inversion.Db;

public class DbTest
{
    public void testAttributeBeautification()
    {
        Db db = new MockDb()
        {
        };

        assertEquals("somecolumn", db.beautifyPropertyName("SOMECOLUMN"));
        assertEquals("someColumn", db.beautifyPropertyName("SomeColumn"));
        assertEquals("sOMEcolumn", db.beautifyPropertyName("SOMEcolumn"));
      assertEquals("someColumn",  db.beautifyPropertyName("SOME_COLUMN"));
      assertEquals("someColumn",  db.beautifyPropertyName("_SOME_COLUMN_"));
      assertEquals("someColumn",  db.beautifyPropertyName("_SOME_  ____COLUMN _ "));
      assertEquals("someColumn",  db.beautifyPropertyName("_some_column_"));
      
      assertEquals("someColumn",  db.beautifyPropertyName(" SOME COLUMN "));
      assertEquals("someColumn",  db.beautifyPropertyName("    SOME    COLUMN   "));
      assertEquals("someColumn",  db.beautifyPropertyName(" some column "));
      assertEquals("someColumn",  db.beautifyPropertyName("        some   column   "));
      
      assertEquals("x23SomeColumn",  db.beautifyPropertyName("123        some   column   "));
      assertEquals("$someColumn",  db.beautifyPropertyName("$some_column"));
      assertEquals("orderDetails",  db.beautifyPropertyName("Order Details"));
      
      
   }
}
