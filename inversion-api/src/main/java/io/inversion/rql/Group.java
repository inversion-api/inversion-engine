/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
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
package io.inversion.rql;

import io.inversion.utils.Utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class Group<T extends Group, P extends Query> extends Builder<T, P> {
    public Group(P query) {
        super(query);
        withFunctions("group");
    }

    public List<String> getGroupBy() {
        List<String> groups = new ArrayList<>();
        for (Term group : findAll("group")) {
            for (Term term : group.getTerms()) {
                if (term.isLeaf())
                    groups.add(term.getToken());
            }
        }
        return groups;
    }

    public T withGroupBy(String... properties) {
        Term group = find("group");
        if (group != null) {
            for (String property : properties) {
                group.withTerm(Term.term(group, property));
            }
        } else {
            withTerm("group", (Object[]) properties);
        }

        return r();
    }
}
