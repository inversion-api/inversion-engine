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
package io.inversion.query;

import io.inversion.ApiException;
import io.inversion.rql.Term;
import io.inversion.utils.Utils;


import java.util.*;

public class Select<T extends Select, P extends Query> extends Builder<T, P> {

    static final Set<String> NON_AGGREGATE_FUNCTIONS = Collections.unmodifiableSet(Utils.add(new LinkedHashSet(), "include", "as", "distinct"));
    static final Set<String> AGGREGATE_FUNCTIONS     = Collections.unmodifiableSet(Utils.add(new LinkedHashSet(), "count", "sum", "min", "max"));


    public Select(P query) {
        super(query);
        withFunctions(NON_AGGREGATE_FUNCTIONS);
        withFunctions(AGGREGATE_FUNCTIONS);
    }

    protected boolean addTerm(String token, Term term) {

        if (AGGREGATE_FUNCTIONS.contains(token)) {

            //-- WHY:
            //-- rewrites something like "max(myCol, 'Max My Col')" to as(max(myCol), 'Max My Col')
            //-- if the optional trailing 'as column' prop is not define then '$$$ANON' is used
            //-- TODO: need test cases for $$$ANON and a more user friendly solution

            String asName = "$$$ANON_" + (getTerms().size() + 1);
            if (term.size() > 1) {
                Term asT = term.getTerm(1);
                term.removeTerm(asT);
                asName = asT.getToken();
            }

            Term as = Term.term(null, "as", term, asName);
            withTerm(as);
            return true;
        } else {
            return super.addTerm(token, term);
        }
    }

    public boolean isDistinct() {
        Term distinct = find("distinct");
        return distinct != null;

    }


    public List<Term> findAggregateTerms() {
        List<Term> aggregates = findAll(AGGREGATE_FUNCTIONS);
        aggregates.removeIf(p -> p.isLeaf());
        return aggregates;
    }

    public List<String> getIncludeColumns() {
        List<String> columns = new ArrayList<>();
        for (Term include : findAll("include")) {
            for (Term child : include.getTerms()) {
                columns.add(child.getToken());
            }
        }

        return columns;
    }

    public Projection getProjection() {
        Projection projection = new Projection();
        for (Term include : findAll("include")) {
            for (Term column : include.getTerms()) {
                if (!column.isLeaf())
                    throw ApiException.new400BadRequest("An include RQL param may not contain nested functions.");

                if (!projection.containsKey(column.getToken()))
                    projection.add(column.getToken(), column);
            }
        }

        if (projection.size() == 0)
            projection.add("*", Term.term(null, "*"));

        for (Term as : findAll("as")) {
            String name = as.getToken(1);
            projection.add(name, as);
        }

        return projection;
    }

}
