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

import java.util.ArrayList;
import java.util.List;

import static io.inversion.context.ContextTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CollectionCodecTest {
    @Test
    public void test_list_unreferenced_by_field_throws_exception(){
        try{
            List<String> bean = Utils.add(new ArrayList<>(), "abc", "[1,2,3]", "asdf\r\nasdf");
            new Context().encode(bean);
            fail("This should have thrown an error because you can not encode a Map that is not referenced by a field");
        }
        catch(Throwable ex){
            ex = Utils.getCause(ex);
            System.out.println(ex.getMessage());
            assertTrue(ex.getMessage().indexOf("Unknowable type") > -1);
        }
    }

    @Test
    public void test_list_of_string(){
        BeanWithListOfStrings bean = new BeanWithListOfStrings("bean");
        Utils.add(bean.list, "ss1,1", "ss1=2");
        assertEncodeDecodeEncodeMatches(bean, "{bean.class=io.inversion.context.BeanWithListOfStrings, bean.list=[ss1\\,1, ss1\\=2]}");
    }

    @Test
    public void test_list_of_beans(){
        BeanWithListOfBeans bean = new BeanWithListOfBeans("bean");
        Utils.add(bean.list, new BeanSimple("value1", "[asdf,asdf]"), new BeanSimple("value2", ",{asdf=asdf}s\r\nb"));
        assertEncodeDecodeEncodeMatches(bean, "{bean.class=io.inversion.context.BeanWithListOfBeans, value1.class=io.inversion.context.BeanSimple, value1.property=[asdf,asdf], value2.class=io.inversion.context.BeanSimple, value2.property=,{asdf=asdf}s\\r\\nb, bean.list=[value1, value2]}");
    }

    @Test
    public void test_list_with_list_of_strings(){
        BeanWithListOfListOfStrings bean = new BeanWithListOfListOfStrings("bean");
        List<String>                l1   = Utils.add(new ArrayList(), "1", "2");
        List<String> l2 = Utils.add(new ArrayList(), "3", "4");
        Utils.add(bean.list, l1, l2);

        assertEncodeDecodeEncodeMatches(bean, "{bean.class=io.inversion.context.BeanWithListOfListOfStrings, bean.list=[[1\\, 2], [3\\, 4]]}");
    }
}
