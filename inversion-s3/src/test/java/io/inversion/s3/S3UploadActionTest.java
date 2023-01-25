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

package io.inversion.s3;

import io.inversion.utils.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class S3UploadActionTest {

    @Test
    public void test_isValidPath(){
        S3UploadAction a = new S3UploadAction();
        assertTrue(a.isValidPath(new Path("hello/world")));
        assertTrue(a.isValidPath(new Path("hello/world.xml")));
        assertTrue(a.isValidPath(new Path("hello/world'xml")));
        assertTrue(a.isValidPath(new Path("hello/world(xml")));
        assertTrue(a.isValidPath(new Path("hello/world)xml")));

        assertFalse(a.isValidPath(new Path("hello/./world!")));
        assertFalse(a.isValidPath(new Path("hello/./")));
        assertFalse(a.isValidPath(new Path("hello/...")));
        assertFalse(a.isValidPath(new Path("hello/world!")));
        assertFalse(a.isValidPath(new Path("hello/world#")));
        assertFalse(a.isValidPath(new Path("hello/world>")));
        assertFalse(a.isValidPath(new Path("hello/world<")));
    }
}
