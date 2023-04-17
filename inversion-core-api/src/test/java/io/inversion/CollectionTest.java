/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
package io.inversion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CollectionTest {

    static final String MANY_TO_ESCAPE = "asdf1234-._()'!*,;-~@%<>[]{}|\\^?/#+&= ---5678910";

//    @Test
//    public void encodeStr_subsequentEncodingsDontChageValue() throws Exception {
//        String source  = MANY_TO_ESCAPE;
//        String encoded = Collection.encodeStr(source);
//        String reencoded = Collection.encodeStr(encoded);
//        assertEquals(encoded, reencoded);
//    }

    @Test
    public void encodeStr_encodeDecodeMustNotModifyValue() throws Exception {
        String source  = "asdf1234-._()'!*,;-~@%<>[]{}|\\^?/#+&= ---5678910";
        String encoded = Collection.encodeStr(source);
        //assertEquals("asdf1234-._()'!*,;-@007e@0040@0025@003c@003e@005b@005d@007b@007d@007c@005c@005e@003f@002f@0023@002b@0026@003d@0020---5678910", encoded);
        assertEquals("asdf1234-._()'!*,;-@007E@0040@0025@003C@003E@005B@005D@007B@007D@007C@005C@005E@003F@002F@0023@002B@0026@003D@0020---5678910", encoded);
        String decoded = Collection.decodeStr(encoded);
        assertEquals(source, decoded);
    }

    @Test
    public void encodeStr_dontEncodeAlwaysSafeUrlCharsOtherThanTildeAndAmpersand() throws Exception {
        String source  = "asdf1234-._()'!*,;";
        String encoded = Collection.encodeStr(source);
        assertEquals(source, encoded);
    }

}
