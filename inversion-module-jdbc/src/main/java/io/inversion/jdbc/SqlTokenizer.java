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
package io.inversion.jdbc;

import io.inversion.utils.Utils;

import java.util.HashSet;
import java.util.Set;

class SqlTokenizer {
    static final Set keywords = new HashSet(Utils.explode(",", "insert,into,update,delete,select,from,where,group,order,limit"));

    final char[] chars;
    int head = 0;

    StringBuilder clause = new StringBuilder();

    StringBuilder token = new StringBuilder();

    boolean escape      = false;
    boolean doubleQuote = false;
    boolean singleQuote = false;
    boolean backQuote   = false;

    public SqlTokenizer(String chars) {
        this.chars = chars.toCharArray();
    }

    boolean quoted() {
        return doubleQuote || singleQuote || backQuote;
    }

    boolean escaped() {
        return escape;
    }

    boolean isAlphaNum(char c) {
        return Character.isAlphabetic(c) || Character.isDigit(c);
    }

    public String nextClause() {
        String toReturn = null;

        String nextToken;
        while ((nextToken = next()) != null) {
            if (keywords.contains(nextToken.toLowerCase())) {
                if (clause.length() > 0) {
                    toReturn = clause.toString();
                    clause = new StringBuilder(nextToken);
                    return toReturn;
                }
            }
            clause.append(nextToken);
        }

        if (clause.length() > 0) {
            toReturn = clause.toString();
            clause = new StringBuilder();
        }

        return toReturn;
    }

    public String next() {
        if (head >= chars.length)
            return null;

        doubleQuote = false;
        singleQuote = false;
        backQuote = false;

        escape = false;

        boolean done   = false;
        int     parens = 0;

        for (; head < chars.length && !done; head++) {
            char c = chars[head];
            switch (c) {
                case '\\':
                    token.append(c);
                    escape = !escape;
                    continue;
                case '(':
                    if (!escaped() && !quoted()) {
                        if (parens == 0 && token.length() > 0) {
                            head--;
                            done = true;
                            break;
                        }

                        parens += 1;
                    }
                    token.append(c);
                    continue;
                case ')':
                    if (!escaped() && !quoted()) {
                        parens -= 1;

                        if (parens == 0) {
                            token.append(c);
                            done = true;
                            break;
                        }
                    }
                    token.append(c);
                    continue;
                case '\"':
                    if (!(escape || singleQuote || backQuote || parens > 0)) {
                        if (!doubleQuote && token.length() > 0) {
                            head--;
                            done = true;
                            break;
                        }

                        doubleQuote = !doubleQuote;

                        if (!doubleQuote) {
                            token.append(c);
                            done = true;
                            break;
                        }
                    }
                    token.append(c);
                    continue;

                case '\'':
                    if (!(escape || doubleQuote || backQuote || parens > 0)) {
                        if (!singleQuote && token.length() > 0) {
                            head--;
                            done = true;
                            break;
                        }

                        singleQuote = !singleQuote;

                        if (!singleQuote) {
                            token.append(c);
                            done = true;
                            break;
                        }
                    }
                    token.append(c);
                    continue;

                case '`':
                    if (!(escape || doubleQuote || singleQuote || parens > 0)) {
                        if (!backQuote && token.length() > 0) {
                            head--;
                            done = true;
                            break;
                        }

                        backQuote = !backQuote;

                        if (!backQuote) {
                            token.append(c);
                            done = true;
                            break;
                        }
                    }
                    token.append(c);
                    continue;

                default:
                    escape = false;

                    if (quoted() || parens > 0) {
                        token.append(c);
                        continue;
                    }

                    if (token.length() > 0) {

                        char previousC = token.charAt(token.length() - 1);

                        if (!isAlphaNum(previousC)) {
                            head--;
                            done = true;
                            break;
                        } else if (isAlphaNum(previousC) && !isAlphaNum(c)) {
                            head--;
                            done = true;
                            break;
                        }
                    }

                    token.append(c);
            }
        }
        String str = token.toString();
        token = new StringBuilder();
        return str;
    }

}