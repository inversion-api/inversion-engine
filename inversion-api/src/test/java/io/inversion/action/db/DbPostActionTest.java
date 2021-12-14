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
package io.inversion.action.db;

import io.inversion.utils.JSArray;
import io.inversion.utils.JSNode;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DbPostActionTest {
    @Test
    public void testCollapses1() {
        JSNode parent = new JSNode();
        parent.put("name", "testing");

        JSNode child1 = new JSNode();
        parent.put("child1", child1);
        child1.put("href", "http://child1");
        child1.put("name", "child1");

        JSNode child2 = new JSNode();
        parent.put("child2", child2);

        child2.put("href", "http://child2");
        child2.put("name", "child2");

        JSNode collapsed = JSNode.asJSNode(parent.toString());

        DbPostAction.collapse(collapsed, false, Collections.singleton("child2"), "");

        JSNode benchmark = JSNode.asJSNode(parent.toString());
        benchmark.remove("child2");
        benchmark.put("child2", new JSNode("href", "http://child2"));

        assertEquals(collapsed.toString(), benchmark.toString());

    }

    @Test
    public void testCollapses2() {
        JSNode parent = new JSNode();
        parent.put("name", "testing");

        JSNode child1 = new JSNode();
        parent.put("child1", child1);
        child1.put("href", "http://child1");
        child1.put("name", "child1");

        JSArray arrChildren = new JSArray();
        for (int i = 0; i < 5; i++) {
            arrChildren.add(new JSNode("href", "href://child" + i, "name", "child" + i));
        }

        parent.put("arrChildren", arrChildren);

        JSNode collapsed = JSNode.asJSNode(parent.toString());

        DbPostAction.collapse(collapsed, false, Collections.singleton("arrChildren"), "");

        JSNode benchmark = JSNode.asJSNode(parent.toString());
        benchmark.remove("arrChildren");
        arrChildren = new JSArray();
        for (int i = 0; i < 5; i++) {
            arrChildren.add(new JSNode("href", "href://child" + i));
        }
        benchmark.put("arrChildren", arrChildren);

        assertEquals(collapsed.toString(), benchmark.toString());

    }

    @Test
    public void testCollapses3() {
        JSNode parent = new JSNode();
        parent.put("name", "testing");

        JSNode child1 = new JSNode();
        parent.put("child1", child1);
        child1.put("href", "http://child1");
        child1.put("name", "child1");

        JSNode child2 = new JSNode();
        parent.put("child2", child2);
        child2.put("href", "http://child2");
        child2.put("name", "child2");

        JSNode child3 = new JSNode();
        child2.put("child3", child3);
        child3.put("href", "http://child3");
        child3.put("name", "child3");

        JSNode collapsed = JSNode.asJSNode(parent.toString());

        DbPostAction.collapse(collapsed, false, Collections.singleton("child2.child3"), "");

        JSNode benchmark = JSNode.asJSNode(parent.toString());
        benchmark.getNode("child2").getNode("child3").remove("name");

        assertEquals(collapsed.toString(), benchmark.toString());

    }

}
