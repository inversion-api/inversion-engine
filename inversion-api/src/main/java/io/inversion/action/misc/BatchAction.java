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
package io.inversion.action.misc;

import io.inversion.Action;


public class BatchAction <T extends BatchAction>  extends Action<T> {

    protected boolean allowBatch = true;

    public boolean isAllowBatch() {
        return allowBatch;
    }

    public T withAllowBatch(boolean allowBatch) {
        this.allowBatch = allowBatch;
        return (T)this;
    }
}
