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

import java.util.*;

public class BeanWithAtomics {

    String name = null;

    String str1 = null;
    String str2 = null;
    String str3 = null;

    boolean b1 = false;
    boolean b2 = false;

    Boolean boolean1 = null;
    Boolean boolean2 = null;

    byte t1    = 0;
    Byte byte1 = 0;

    char      c1         = '0';
    Character character1 = null;

    short s1     = -1;
    Short short1 = -1;

    int i1       = -1;
    int integer1 = -1;

    long l1    = -1;
    long long1 = -1;

    float f1     = -1;
    Float float1 = -1f;

    float  d1      = -1;
    Double double1 = -1d;

    public BeanWithAtomics(String name){
        this.name = name;
    }

    public BeanWithAtomics(){

    }

    public String getName() {
        return name;
    }
}
