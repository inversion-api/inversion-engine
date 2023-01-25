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

import static io.inversion.context.ContextTest.assertEncodeDecodeEncodeMatches;

public class CodecTest {

    @Test
    public void test_list_of_string(){
        BeanIsCodec codecBean = new BeanIsCodec("codecBean", "value");
        BeanWithBeanIsCodec bean = new BeanWithBeanIsCodec("bean", "othervalue");
        bean.codecBean = codecBean;
        assertEncodeDecodeEncodeMatches(bean, "{bean.class=io.inversion.context.BeanWithBeanIsCodec, bean.property=othervalue, bean.codecBean=Hello World: value}");
    }

}
