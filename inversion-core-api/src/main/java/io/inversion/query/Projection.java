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

package io.inversion.query;

import io.inversion.rql.Term;
import io.inversion.utils.LinkedCaseInsensitiveMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Projection {

    //ALL("ALL"),
    //KEYS_ONLY("KEYS_ONLY"),
    //INCLUDE("INCLUDE");

    String                   type  = "KEYS_ONLY";
    LinkedCaseInsensitiveMap terms = new LinkedCaseInsensitiveMap();



    public int size(){
        return terms.size();
    }

    public Set<String> keySet(){
        return terms.keySet();
    }

    public boolean containsKey(String key){
        return terms.containsKey(key);
    }

    public Projection add(String column){
        terms.put(column, Term.term(null, column));
        return this;
    }

    public Projection add(String key, Term term){
        terms.put(key, term);
        return this;
    }

    public Term get(String key){
        return (Term)terms.get(key);
    }

    public List<Term> getTerms(){
        return new ArrayList(terms.values());
    }

    public String getType() {
        return type;
    }

    public Projection withType(String type) {
        this.type = type;
        return this;
    }

}
