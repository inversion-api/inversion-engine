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
package io.inversion.cloud.service;

import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import io.inversion.cloud.model.Collection;
import junit.framework.TestCase;

public class TestKeyEncoding extends TestCase
{
   @Test
   public void test1() throws Exception
   {
      String string = new String("kdksks*/sl&(2)".getBytes(), "UTF-8");

      System.out.println("string   : '" + string + "'");

      String encoded = Collection.encodeStr(string);
      System.out.println("encoded  : '" + encoded + "'");

      String decoded = Collection.decodeStr(encoded);
      System.out.println("decoded  : '" + decoded + "'");

      String reincoded = Collection.encodeStr(decoded);
      System.out.println("reincoded: '" + reincoded + "'");

      assertEquals(string, decoded);
      assertNotEquals(string, encoded);
   }
}
