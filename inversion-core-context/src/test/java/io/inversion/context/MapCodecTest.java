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

import io.inversion.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.inversion.context.ContextTest.assertEncodeDecodeEncodeMatches;
import static org.junit.jupiter.api.Assertions.*;

public class MapCodecTest {

    @Test
    public void test_map_unreferenced_by_field_throws_exception() {
        try {
            Map<String, String> bean = Utils.addToMap(new LinkedHashMap(), "abc", "123", "{def=456}", "yep");
            new Context().encode(bean);
            fail("This should have thrown an error because you can not encode a Map that is not referenced by a field");
        } catch (Throwable ex) {
            ex = Utils.getCause(ex);
            System.out.println(ex.getMessage());
            assertTrue(ex.getMessage().indexOf("Unknowable type") > -1);
        }
    }

    @Test
    public void test_map_string_to_string() {
        BeanWithMapOfStringToString bean = new BeanWithMapOfStringToString("bean");
        Utils.addToMap(bean.map, "ss1,1", "ss1=2", "asdf", "123");
        assertEncodeDecodeEncodeMatches(bean, "{bean.class=io.inversion.context.BeanWithMapOfStringToString, bean.map={ss1\\,1=ss1\\=2, asdf=123}}");
    }

    @Test
    public void test_map_string_to_bean() {
        BeanWithMapOfStringToBean bean = new BeanWithMapOfStringToBean("bean");
        Utils.addToMap(bean.map, "ss1,1", new BeanSimple("simpleBean", "{as,sd=as,df}"), "as=bs,ds", new BeanSimple("simpleBean2", "123"));
        assertEncodeDecodeEncodeMatches(bean, "{bean.class=io.inversion.context.BeanWithMapOfStringToBean, simpleBean.class=io.inversion.context.BeanSimple, simpleBean.property={as,sd=as,df}, simpleBean2.class=io.inversion.context.BeanSimple, simpleBean2.property=123, bean.map={ss1\\,1=simpleBean, as\\=bs\\,ds=simpleBean2}}");
    }

    @Test
    public void test_map_bean_to_bean() {
        BeanWithMapOfBeanToBean bean = new BeanWithMapOfBeanToBean("bean");
        Utils.addToMap(bean.map, new BeanSimple("key1", "prop"), new BeanSimple("value1", "prop"),new BeanSimple("key2", "prop1"), new BeanSimple("value2", "prop") );
        assertEncodeDecodeEncodeMatches(bean, "{bean.class=io.inversion.context.BeanWithMapOfBeanToBean, key1.class=io.inversion.context.BeanSimple, key1.property=prop, value1.class=io.inversion.context.BeanSimple, value1.property=prop, key2.class=io.inversion.context.BeanSimple, key2.property=prop1, value2.class=io.inversion.context.BeanSimple, value2.property=prop, bean.map={key1=value1, key2=value2}}");
    }
}
