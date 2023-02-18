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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DbPostActionTest {
//    @Test
//    public void testCollapses1() {
//        JSMap parent = new JSMap();
//        parent.putValue("name", "testing");
//
//        JSMap child1 = new JSMap();
//        parent.putValue("child1", child1);
//        child1.putValue("href", "http://child1");
//        child1.putValue("name", "child1");
//
//        JSMap child2 = new JSMap();
//        parent.putValue("child2", child2);
//
//        child2.putValue("href", "http://child2");
//        child2.putValue("name", "child2");
//
//        JSNode collapsed = JSReader.asJSNode(parent.toString());
//
//        DbPostAction.collapse(collapsed, false, Collections.singleton("child2"), "");
//
//        JSNode benchmark = JSReader.asJSNode(parent.toString());
//        benchmark.remove("child2");
//        benchmark.putValue("child2", new JSMap("href", "http://child2"));
//
//        assertEquals(collapsed.toString(), benchmark.toString());
//
//    }
//
//    @Test
//    public void testCollapses2() {
//        JSNode parent = new JSMap();
//        parent.putValue("name", "testing");
//
//        JSNode child1 = new JSMap();
//        parent.putValue("child1", child1);
//        child1.putValue("href", "http://child1");
//        child1.putValue("name", "child1");
//
//        JSList arrChildren = new JSList();
//        for (int i = 0; i < 5; i++) {
//            arrChildren.add(new JSMap("href", "href://child" + i, "name", "child" + i));
//        }
//
//        parent.putValue("arrChildren", arrChildren);
//
//        JSNode collapsed = JSReader.asJSNode(parent.toString());
//
//        DbPostAction.collapse(collapsed, false, Collections.singleton("arrChildren"), "");
//
//        JSNode benchmark = JSReader.asJSNode(parent.toString());
//        benchmark.remove("arrChildren");
//        arrChildren = new JSList();
//        for (int i = 0; i < 5; i++) {
//            arrChildren.add(new JSMap("href", "href://child" + i));
//        }
//        benchmark.putValue("arrChildren", arrChildren);
//
//        assertEquals(collapsed.toString(), benchmark.toString());
//
//    }
//
//    @Test
//    public void testCollapses3() {
//        JSNode parent = new JSMap();
//        parent.putValue("name", "testing");
//
//        JSNode child1 = new JSMap();
//        parent.putValue("child1", child1);
//        child1.putValue("href", "http://child1");
//        child1.putValue("name", "child1");
//
//        JSNode child2 = new JSMap();
//        parent.putValue("child2", child2);
//        child2.putValue("href", "http://child2");
//        child2.putValue("name", "child2");
//
//        JSNode child3 = new JSMap();
//        child2.putValue("child3", child3);
//        child3.putValue("href", "http://child3");
//        child3.putValue("name", "child3");
//
//        JSNode collapsed = JSReader.asJSNode(parent.toString());
//
//        DbPostAction.collapse(collapsed, false, Collections.singleton("child2.child3"), "");
//
//        JSNode benchmark = JSReader.asJSNode(parent.toString());
//        benchmark.getNode("child2").getNode("child3").remove("name");
//
//        assertEquals(collapsed.toString(), benchmark.toString());
//
//    }

}
