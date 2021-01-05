/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
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
package io.inversion.rql;

import java.util.ArrayList;
import java.util.List;

public class Order<T extends Order, P extends Query> extends Builder<T, P> {

    List<Sort> sorts = new ArrayList();

    public Order(P query) {
        super(query);
        withFunctions("order", "sort");
    }

    /**
     * @param index the sort term to check
     * @return true if the indexed sort is ascending or if the index is out of bounds
     */
    public boolean isAsc(int index) {
        List<Sort> sorts = getSorts();
        return sorts.size() <= index || sorts.get(index).isAsc();
    }

    public String getProperty(int index) {
        List<Sort> sorts = getSorts();
        return sorts.size() <= index ? null : sorts.get(index).getProperty();
    }

    public List<Sort> getSorts() {
        if(sorts.size() < 1){
            List<Sort> sorts = new ArrayList<>();
            for (Term term : getTerms()) {
                if (term.hasToken("sort", "order")) {
                    for (Term child : term.getTerms()) {
                        String  property = child.token;
                        boolean asc      = true;
                        if (property.startsWith("-")) {
                            asc = false;
                            property = property.substring(1);
                        } else if (property.startsWith("+")) {
                            property = property.substring(1);
                        }
                        sorts.add(new Sort(property, asc));
                    }
                }
            }
        }
        return sorts;
    }

    public Order withSorts(List<Sort> sorts){
        this.sorts.addAll(sorts);
        return this;
    }

    public static class Sort {
        String  property;
        boolean asc;

        public Sort(String property, boolean asc) {
            super();
            this.property = property;
            this.asc = asc;
        }

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }

        public boolean isAsc() {
            return asc;
        }

        public void setAsc(boolean asc) {
            this.asc = asc;
        }
    }
}
