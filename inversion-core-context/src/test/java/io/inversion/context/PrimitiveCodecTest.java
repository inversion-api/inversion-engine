/*
 * Copyright (c) 2015-2022 Rocket Partners, LLC
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

package io.inversion.context;

import org.junit.jupiter.api.Test;

import static io.inversion.context.ContextTest.assertEncodeDecodeEncodeMatches;

public class PrimitiveCodecTest {

    @Test
    public void test_list_of_string(){
        BeanWithAtomics bean = new BeanWithAtomics("bean");
        bean.str1 = "hello";
        bean.str2 = "world";
        bean.str3 = "again";
        bean.b1 = true;
        bean.b2 = true;
        bean.boolean1 = true;
        bean.boolean2 = false;
        bean.t1 = 1;
        bean.byte1 = 2;
        bean.c1 = 'a';
        bean.character1 = 'b';
        bean.s1 = 3;
        bean.short1=4;
        bean.i1 = 5;
        bean.integer1=6;
        bean.l1 = 7;
        bean.long1 = 8;
        bean.f1 = 9;
        bean.float1 = 10f;
        bean.d1 = 11;
        bean.double1 = 12d;
        assertEncodeDecodeEncodeMatches(bean, "{bean.class=io.inversion.context.BeanWithAtomics, bean.str1=hello, bean.str2=world, bean.str3=again, bean.b1=true, bean.b2=true, bean.boolean1=true, bean.boolean2=false, bean.t1=1, bean.byte1=2, bean.c1=a, bean.character1=b, bean.s1=3, bean.short1=4, bean.i1=5, bean.integer1=6, bean.l1=7, bean.long1=8, bean.f1=9.0, bean.float1=10.0, bean.d1=11.0, bean.double1=12.0}");
    }
}
