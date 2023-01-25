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
package io.inversion.utils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * A case insensitive utility abstraction for working with forward slash based paths /like/you/find/in/urls.
 * <p>
 * When working with Paths, leading and trailing slashes are completely disregarded.
 * Any leading and trailing slashes in the documentation are illustrative only and could be excluded to achieve the same result.
 * Multiple consecutive slashes are treated as a single slash.
 * There will never be an empty string or null path part.
 * <p>
 * In addition to representing concrete paths, such as a file path or url path, a Path object may
 * contain wildcards, regular expressions, and variable name bindings which are used when comparing a variablized
 * abstract paths with a concrete path.
 * <p>
 * Paths are primarily used to configure Rules (Engine, Api, Endpoint and Action are all subclasses of Rule)
 * to match against inbound Request urls to determine how the Request will be processed.
 * <p>
 * Paths can be variablized as follows:
 * <ul>
 *  <li>/animals/* - will match anything that starts with "animals/".
 *      "*" matches any number of path segments including zero segments (meaning "animals/" alone will match and so will "animals/dogs/fido").
 *      "*" wildcards are only valid as the last segment in a path.
 *
 *  <li>/animals/dogs/:dogName - if a path segment starts with a ":" it indicates that the value can be anything but a value is required and should be
 *      mapped to the corresponding variable name, in this case "dogName", by whoever is doing the path matching.
 *
 *  <li>/animals/dogs/{fido|ralph|jackie} - if the path segment is wrapped in "{}" the contents are considered a regular expression {@link java.util.regex.Pattern} for matching
 *
 *  <li>/animals/dogs/${fido|ralph|jackie} - a '$' can prefix '{}' as syntatic sugar some may be familiar with.
 *
 *  <li>/animals/dogs/{dogName:fido|ralph|jackie} - again a regex, this time with a variable binding to "dogName"
 *
 *  <li>/animals/[{dogs|cats|snakes}]/:animalName/* - if something is wrapped in square brackets "[]" that segment and all subsequent segments
 *      are optional.  If the segments exists in the path being compared to, they must match the supplied rules, but if the
 *      comparison path ends right before the first optional segment, the paths still match.
 *
 *  </ul>
 * <p>
 * When used in the context of a Api configuration you may see something like this:
 *
 * <pre>
 *  Engine e = new Engine().withIncludeOn(null, new Path("/apis"));
 *                         .withApi(new Api().withIncludeOn(null, new Path("bookstore/{storeName:johnsBestBooks|carolsBooksOnMain}"))
 *                                           .withEndpoint(new Endpoint().withIncludeOn(null, new Path("categories/:category/"))
 *                                                                       .withAction(new BrowseCategoriesAction().withIncludeOn(null, new Path("[:subcategory]/*")))));
 * </pre>
 */

public class Path implements Comparable<Path> {
    List<String> parts = new ArrayList<>();
    List<String> lc    = new ArrayList<>();

    public String getTemplate(){
        StringBuilder b = new StringBuilder();
        for(int i=0; i<size(); i++){
            String part = get(i).toLowerCase();
            if(isVar(i)){
                part = "{}";
                String regex = getRegex(i);
                if(regex != null)
                  part = "{" + regex + "}";
            }
            if(b.length() > 0)
                b.append("/");

            b.append(part);
        }
        return b.toString();
    }


    /**
     * Creates an empty Path
     */
    public Path() {

    }

    /**
     * Creates a clone of the supplied <code>Path</code>
     *
     * @param path the Path to be cloned
     */
    public Path(Path path) {
        copyFrom(path);
    }

    /**
     * Constructs a Path based on all of the supplied <code>part</code>s.
     * <p>
     * The strings in <code>part</code> may themselves contain "/" characters and will be split into multiple parts correspondingly.
     * Meaning <code>Path p = new Path("part1", "part2/part3", "/part4/", "////part5//part6/", "part7")</code> is valid
     * and would result in a Path with parts "part1", "part2", "part3", "part4", "part5", "part6", "part7".
     *
     * @param part an array of path part strings
     */
    public Path(String... part) {
        for (String s : part) {
            add(s);
        }
    }

    /**
     * Convenience overload of {@link #Path(String...)}.
     *
     * @param parts an list of path part strings
     */
    public Path(List<String> parts) {
        this(parts.toArray(new String[0]));
    }

    public static List<Path> expandOptionals(List<Path> paths) {
        List<Path> allPaths = new ArrayList();
        for (Path path : paths)
            allPaths.addAll(path.getSubPaths());
        return allPaths;
    }

    public static List<Path> filterDuplicates(List<Path> paths) {
        List<Path> allPaths = new ArrayList();
        for (Path path : paths)
            allPaths.addAll(path.getSubPaths());

        for (int i = 0; i < allPaths.size(); i++) {
            for (int j = i + 1; j < allPaths.size(); j++) {
                Path p1 = allPaths.get(i);
                Path p2 = allPaths.get(j);

                if (p1.size() != p2.size())
                    continue;

                boolean same = true;

                for (int k = 0; same && k < p1.size(); k++) {
                    if (p1.isVar(k) && p2.isVar(k))
                        continue;
                    else if (p1.isWildcard(k) && p2.isWildcard(k))
                        continue;
                    else {
                        String part1 = p1.get(k);
                        String part2 = p2.get(k);
                        if (!part1.equalsIgnoreCase(part2)) {
                            same = false;
                        }
                    }
                }
                if (same) {
                    allPaths.remove(j);
                    j -= 1;
                }
            }
        }

        return allPaths;
    }

    public static List<Path> materializeTrivialRegexes(List<Path> paths) {
        List<Path> fixed = new ArrayList<>();
        for (int j = 0; j < paths.size(); j++) {
            Path p = paths.get(j);

            boolean swapped = false;
            for (int i = 0; i < p.size(); i++) {
                String regex = p.getRegex(i);
                if (regex != null) {
                    boolean      allSimple = true;
                    List<String> parts     = Utils.split(regex, '|');
                    for (String s : parts) {
                        if (Utils.isRegex(s)) {
                            allSimple = false;
                            break;
                        }
                    }
                    if (allSimple) {
                        swapped = true;
                        paths.remove(j);
                        j--;
                        List<Path> updated = new ArrayList<>();
                        for (String part : parts) {
                            Path copy = p.copy();
                            copy.set(i, part);
                            updated.add(copy);
                        }
                        paths.addAll(j + 1, updated);
                        break;
                    } else {
                        p.set(i, p.getVarName(i));
                    }
                }
            }
            if (!swapped)
                fixed.add(p);
        }
        return fixed;
    }

    public Path copyFrom(Path path) {
        if(path == null)
            return this;

        parts.clear();
        lc.clear();
        parts.addAll(path.parts);
        lc.addAll(path.lc);
        return this;
    }

    /**
     * Return a new path that is an exactly copy of this one.
     *
     * @return
     */
    public Path copy() {
        Path copy = new Path();
        copy.parts.addAll(parts);
        copy.lc.addAll(lc);
        return copy;
    }

    /**
     * Gets the path parts as a List.
     * <p>
     * Method signature could easily have been "asList()"
     *
     * @return a new list with the individual path parts n the originally defined case.
     */
    public List<String> parts() {
        return new ArrayList<>(parts);
    }

    /**
     * Simple way to pull the first element of the path without having to check for <code>size() &gt; 0</code> first.
     *
     * @return the first element in the path if it exists otherwise null
     */
    public String first() {
        if (parts.size() > 0)
            return parts.get(0);
        return null;
    }

    /**
     * Simple way to pull the last element of the path without having to check for <code>size() &gt; 0</code> first.
     *
     * @return the last element in the path if it exists otherwise null
     */
    public String last() {
        if (parts.size() > 0)
            return parts.get(parts.size() - 1);
        return null;
    }

    /**
     * Simple way to get element at <code>index</code> without haveint to check for <code>size() &gt; index</code> first.
     *
     * @param index the index of the path part to retrive
     * @return the path part at index if it exists otherwise null
     */
    public String get(int index) {
        if (index < parts.size())
            return parts.get(index);
        return null;
    }

    /**
     * Adds <code>part</code> to the end of the Path.
     * <p>
     * The <code>parts</code> is exploded via <code>Utils.explode('/', part)</code> first so while the part arg is a
     * single value, it could result in multiple additions.
     *
     * @param parts path parts to add
     */
    public Path add(String parts) {

        String debug = this.size() == 0 ? parts : Utils.implode("/", this.toString(), parts);

        if (!Utils.empty(parts)) {
            for (String part : Utils.explode("/", parts)) {

                boolean isOptional = part.startsWith("[");
                if (isOptional) {
                    part = part.substring(1);
                    if (part.endsWith("]"))
                        part = part.substring(0, part.length() - 1);
                    part = part.trim();
                }

                //-- don't accumulate additional "*"
                if ("*".equals(part) && endsWithWildcard())
                    continue;//

                if (part.startsWith(":") || part.startsWith("$"))
                    throw Utils.ex("A path part may not start with a ':' or '$'.");

                boolean isVar = part.startsWith("{");
                if (isVar) {
                    part = part.substring(1);
                    if (part.endsWith("}"))
                        part = part.substring(0, part.length() - 1);
                }

                //if(!isVar && part.contains(":"))
                //    throw new ApiException("Invalid path segment '{}'.  A ':' can only be used in a Path if the segment is a variable wrapped in curly brackets.", parts);

                boolean isRegx = isVar && part.contains(":");
                if (isRegx) {
                    String name  = part.substring(0, part.indexOf(":"));
                    String regex = part.substring(part.indexOf(":"));
                    if (name.contains("[") || name.contains("{") || name.contains("]") || name.contains("}"))
                        throw Utils.ex("Invalid path segment '{}'.", parts);
                }

                if (isVar)
                    part = "{" + part + "}";

                if (isOptional)
                    part = "[" + part + "]";

                if (size() > 0 && isWildcard(size() - 1))
                    throw Utils.ex("Invalid path '{}'.  Wildcards can only be used as the last segment in a path.", debug);

                this.parts.add(part);
                lc.add(part.toLowerCase());
            }
        }
        return this;
    }

    public Path set(int index, String part) {
        parts.set(index, part);
        lc.set(index, part.toLowerCase());
        return this;
    }

    public Path chop() {
        if (size() > 0) {
            return subpath(0, size() - 1);
        }
        return this;
    }

    /**
     * Simple way to remove the path part at <code>index</code> without having to check for <code>size() @lt; index</code> first.
     *
     * @param index the index of the path part to remove
     * @return the path part previously located at <code>index</code> if it existed otherwise null
     */
    public String remove(int index) {
        if (index < parts.size()) {
            lc.remove(index);
            return parts.remove(index);
        }
        return null;
    }

    public Path removeLast(){
        remove(size()-1);
        return this;
    }

    /**
     * @return true if this Path ended in a "*" which was removed, false if this Path does not end in a "*"
     */
    public boolean removeTrailingWildcard() {
        if (size() > 0 && isWildcard(size() - 1)) {
            remove(size() - 1);
            return true;
        }
        return false;
    }

    /**
     * @return true if this Path ends with a "*"
     */
    public boolean endsWithWildcard() {
        if (size() > 0 && isWildcard(size() - 1)) {
            return true;
        }
        return false;
    }

    /**
     * Performs a case insensitive string match between this Path and <code>pathsToMatch</code>.
     * <p>
     * Wildcards and regular expressions are not supported in this method, only straight case insensitive string comparison.
     *
     * @param partsToMatch the path parts to to match against
     * @return true if each index of <code>partsToMatch</code> is a case insensitive match to this Path at the same index otherwise false.
     */
    public boolean startsWith(List<String> partsToMatch) {
        if (partsToMatch.size() > this.parts.size())
            return false;

        for (int i = 0; i < partsToMatch.size(); i++) {
            if (!partsToMatch.get(i).equalsIgnoreCase(this.parts.get(i)))
                return false;
        }
        return true;
    }

    /**
     * @return the number of parts in the Path
     */
    public int size() {
        return parts.size();
    }

    /**
     * @return a pretty printed "/" separated path string representation
     */
    public String toString() {
        return Utils.implode("/", parts);
    }

    public int hashCode() {
        return toString().toLowerCase().hashCode();
    }

    @Override
    public int compareTo(Path o) {
        if (o == null)
            return 1;
        return toString().toLowerCase().compareTo(o.toString().toLowerCase());
    }


    /**
     * @return true of the objects string representations match
     */
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (!(o instanceof Path))
            return false;

        return toString().equalsIgnoreCase(o.toString());
    }

    public static String unwrapOptional(String part) {
        while (part.startsWith("["))
            part = part.substring(1);
        while (part.endsWith("]"))
            part = part.substring(0, part.length() - 1);
        return part;
    }

    /**
     * Creates a new sub Path.
     *
     * @param fromIndex low endpoint (inclusive) of the subList
     * @param toIndex   high endpoint (exclusive) of the subList
     * @return a subpath from <code>fromIndex</code> (inclusive) to <code>toIndex</code> (exclusive)
     */
    public Path subpath(int fromIndex, int toIndex) {
        return new Path(parts.subList(fromIndex, toIndex));
    }

    /**
     * Checks to see if the value at <code>index</code> is a wildcard, a variable, or is optional.
     *
     * @param index the path part to check
     * @return true if the path part at <code>index</code> is a '*' char or starts with '[', '{' or ':'
     */
    public boolean isStatic(int index) {
        return !(isWildcard(index) || isVar(index) || isOptional(index));
    }

    /**
     * Check if the path part at <code>index</code> is equal to '*' without having
     * to check if <code>size() @lt; index</code> first.
     *
     * @param index the path part to check
     * @return true if the path part at <code>index</code>
     */
    public boolean isWildcard(int index) {
        return isWildcard(get(index));
    }

    public static boolean isWildcard(String part) {
        return "*".equals(part);
    }

    /**
     * @return true if this path equals "*"
     */
    public boolean isWildcard() {
        return size() == 1 && endsWithWildcard();
    }

    /**
     * Check to see if the value at <code>index</code> starts with '${', '{', ':' after removing any leading '[' characters.
     *
     * @param index the index to check
     * @return true if the value exists and is variableized but not a wildcard, false otherwise.
     */
    public boolean isVar(int index) {
        String part = get(index);
        return isVar(part);
    }

    public static boolean isVar(String part) {
        if (part != null) {
            if (part.startsWith("["))
                part = part.substring(1).trim();

            char c = part.charAt(0);
            return c == '{';
        }
        return false;
    }

    /**
     * Extracts a variable name form the path expression if <code>index</code> exists and has a var name.
     * <p>
     * 'varName' would be extracted from <code>getVarName(1)</code> for the following paths.
     * <ul>
     *  <li> /part/:varName/
     *  <li> /part/{varNam:regex}/
     *  <li> /part/${varNam:regex}/
     * </ul>
     * <p>
     * Square brackets indicating a path component is optioanl don't impact retrieval of the var name so the following
     * would return the same as there above counterparts:
     * <ul>
     *  <li> /part/[:varName]/
     *  <li> /[part]/{varNam:regex}]/
     *  <li> /[part]/[${varNam:regex}]/
     * </ul>
     *
     * @param index the index of the var name to get
     * @return the variable name binding for the parth part at <code>index</code> if it exists
     */
    public String getVarName(int index) {
        String part = get(index);
        if (part != null) {
            if (part.startsWith("["))
                part = part.substring(1, part.length() - 1);

            if (part.startsWith("{")) {
                int colon = part.indexOf(":");
                if (colon > 0)
                    return part.substring(1, colon).trim();
                else
                    return part.substring(1, part.lastIndexOf("}"));
            }
        }
        return null;
    }

    //TODO DOCUMENT ME
    public String getRegex(int index) {
        String part = get(index);
        return getRegex(part);
    }

    public static String getRegex(String part) {
        int colon = part.indexOf(":");
        if (colon > 0)
            return part.substring(colon + 1, part.lastIndexOf("}")).trim();

        return null;
    }

    /**
     * Square brackets, '[]', indicate that a path path (and by implication, all following parts) are considered optional for path matching.
     * <p>
     * For example: <code>new Path("first/second/[third]/").matches(new Path("first/second/third"))</code>
     *
     * @param index the part part to check
     * @return true if the path part at <code>index</code> exists and starts with '[' and ends with ']'
     */
    public boolean isOptional(int index) {
        String part = get(index);
        return isOptional(part);
    }

    public static boolean isOptional(String part) {
        if (part != null) {
            return part.startsWith("[") && part.endsWith("]");
        }
        return false;
    }

    public void setOptional(int index, boolean optional) {
        if (index < size()) {
            String part = get(index);
            part = unwrapOptional(part);

            if (optional) {
                part = "[" + part + "]";
            }
            set(index, part);
        }
    }

    /**
     * Convenience overloading of {@link #matches(Path)}.
     *
     * @param toMatch the path string to match
     * @return true if the paths match
     */
    public boolean matches(String toMatch) {
        return matches(new Path(toMatch));
    }


    /**
     * Checks if this Path is a case insensitive match, including any optional rules, wildcards, and regexes to <code>concretePath</code>.
     * <p>
     * As the name implies <code>concretePath</code> is considered to be a literal path not containing optionals, wildcards, and regexes.
     * <p>
     * As also documented above:
     * <ul>
     *   <li>paths can end with a "/*" character meaning this and all following parts are optional and unconstrained
     *   <li>wrapping a path part in square brackets, '[]' indicates that it and all following part parts are optional.
     *   <li>wrapping a path part in '${}' or '${}' indicates that this path part should match via a regular expression such as '${[0-9a-fA-F]{1,8}}' to match a 1 to 8 character alpha numeric part
     *   <li>you can bind a variable name to a regex by preceding the regex with a name and a colon '/${id:[0-9a-fA-F]{1,8}}/'
     *   <li>starting a path part with a ':' such as '/:id/' is functionally equivalent to '/${id:.*}'
     * </ul>
     * <p>
     * All non regex comparisons are performed with String.equalsIgnoreCase.
     * <p>
     * All regexes are compiled with Pattern.CASE_INSENSITIVE.
     *
     * @param toMatch the path to match against
     * @return true if this path matches <code>concretePath</code>
     */
    public boolean matches(Path toMatch) {
        return matches(toMatch, false);
    }

    public boolean matches(Path toMatch, boolean bidirectional) {

        int  len = Math.max(size(), toMatch.size());
        Path a   = this;
        Path b   = toMatch;

        boolean aOptional = false;
        boolean bOptional = false;
        for (int i = 0; i < len; i++) {
            String aVal = a.get(i);
            String bVal = b.get(i);

            if ("*".equals(aVal) || "*".equals(bVal))
                return true;

            aOptional = aOptional || (aVal != null && aVal.startsWith("["));
            bOptional = bOptional || (bVal != null && bVal.startsWith("["));

            if (aVal == null || bVal == null)
                return aOptional || bOptional;

            if (a.isVar(i) && b.isVar(i)) {
                continue;
            } else if (a.isVar(i)) {
                String regex = a.getRegex(i);
                if (regex != null) {
                    String value = b.get(i);
                    value = !value.startsWith("[") ? value : value.substring(1, value.length() - 1);
                    Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                    if (!pattern.matcher(value).matches())
                        return false;
                }
            } else if (bidirectional && b.isVar(i)) {
                String regex = b.getRegex(i);
                if (regex != null) {
                    String value = a.get(i);
                    value = !value.startsWith("[") ? value : value.substring(1, value.length() - 1);
                    Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                    if (!pattern.matcher(value).matches())
                        return false;
                }
            } else {
                aVal = !aVal.startsWith("[") ? aVal : aVal.substring(1, aVal.length() - 1);
                bVal = !bVal.startsWith("[") ? bVal : bVal.substring(1, bVal.length() - 1);
                if (!aVal.equalsIgnoreCase(bVal))
                    return false;
            }
        }
        return true;
    }


    /**
     * Convenience overloading of {@link #extract(Map, Path, boolean)} with <code>greedy = true</code>.
     *
     * @param params  a map to add extracted name/value pairs to
     * @param toMatch the path to extract from
     * @return the part of this path that matched
     * @see Path#extract(Map, Path, boolean)
     */
    public Path extract(Map params, Path toMatch) {
        return extract(params, toMatch, false);
    }

    /**
     * Consumes the matching parts of <code>matchingConcretePath</code> and extracts any named variable to <code>matchingConcretePath</code> if this
     * any of this paths parts contain variable bindings.
     * <p>
     * If <code>greedy</code> is true, the match will consume through matching optional path parts.
     * <p>
     * If <code>greedy</code> is false, variable bindings in any optional paths parts will be extracted but the parts will not be removed from <code>matchingConcretePath</code>.
     * <p>
     * Here is an example:
     * <pre>
     *   Map params = new HashMap();
     *   Path engineMatch = new Path("apis/myapi/*");
     *   Path apiMatch = new Path("${version:v1|v2}/:tenant")
     *   Path endpointMatch = new Path("[${collection:books|categories|orders}]/*");
     *   Path actionMatch = new Path("[:orderId]/*");
     *
     *   Path requestPath = new Path("/apis/myapi/v2/bobsBooks/orders/67890");
     *
     *   engineMatch.extract(requestPath);
     *   // params is empty, requestPath is now 'v2/bobsBooks/orders/67890'
     *
     *   apiMatch.extract(requestPath);
     *   //version=v2 and tenant=bobsBooks have been added to params and requestPath is now 'orders/67890'
     *
     *   endpointMatch.extract(requestPath);
     *   //collection=orders has been added to params and requestPath is now '67890'
     *
     *   actionMatch.extract(requestPath);
     *   //orderId=67890 has been added to params and requestPath is now empty.
     * </pre>
     * <p>
     * Engine will also add the params to the Request Url params as if they had been supplied as name value pairs by the caller on the query string.
     *
     * @param params               the map to add extracted name/value pairs to
     * @param matchingConcretePath the path to extract from
     * @param greedy               if extraction should consume through optional path parts
     * @return the same Path object that was passed in but potentially now shorter as matching segments may have been consumed
     */
    public Path extract(Map params, Path matchingConcretePath, boolean greedy) {
        Path matchedPath = new Path();

        boolean restOptional = false;
        int     i;
        int     nextOptional = 0;
        for (i = 0; i < size() && matchingConcretePath.size() > 0; i++) {
            String myPart = get(i);

            boolean partOptional = myPart.startsWith("[") && myPart.endsWith("]");

            if (partOptional) {
                restOptional = true;
                myPart = myPart.substring(1, myPart.length() - 1);
            }

            if (myPart.equals("*"))
                break;

            String theirPart;

            if (greedy || !restOptional) {
                theirPart = matchingConcretePath.remove(0);
                matchedPath.add(theirPart);
            } else {
                theirPart = matchingConcretePath.get(nextOptional++);
            }

            if (isVar(i)) {
                String name  = getVarName(i);
                String regex = getRegex(i);
                if (regex != null) {
                    Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                    if (!pattern.matcher(theirPart).matches()) {
                        throw Utils.ex("Attempting to extract values from an unmatched path: '{}', '{}'", this, matchingConcretePath.toString());
                    }
                }
                params.put(name, theirPart);

            } else if (!myPart.equalsIgnoreCase(theirPart)) {
                throw Utils.ex("Attempting to extract values from an unmatched path: '{}', '{}'", this, matchingConcretePath.toString());
            }
        }

        //null out any trailing vars
        for (; i < size(); i++) {
            String var = getVarName(i);
            if (var != null)
                params.put(var, null);
        }

        return matchedPath;
    }

    public int getVarIndex(String varName) {
        if (varName == null)
            return -1;

        for (int i = 0; i < size(); i++) {
            String temp = getVarName(i);
            if (varName.equalsIgnoreCase(temp))
                return i;
        }
        return -1;
    }

    public boolean hasVars() {
        for (int i = 0; i < size(); i++)
            if (isVar(i))
                return true;
        return false;
    }

    public boolean hasAllVars(String... vars) {
        for (int i = 0; i < vars.length; i++) {
            boolean has = false;
            for (int j = 0; j < size(); j++) {
                if (vars[i].toString().equalsIgnoreCase(getVarName(j))) {
                    has = true;
                    break;
                }
            }
            if (!has) {
                return false;
            }
        }
        return true;
    }

    public boolean hasAnyVars(String... vars) {
        for (int i = 0; i < vars.length; i++) {
            for (int j = 0; j < size(); j++) {
                if (vars[i].toString().equalsIgnoreCase(getVarName(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Creates a list of all subpaths breaking before each optional segment
     * Ex:
     * <pre>
     *     a/b[c]/d/[e]/*
     * </pre>
     * Becomes:
     * <pre>
     *     a/b
     *     a/b/c/d
     *     a/b/c/d/e/*
     * </pre>
     *
     * @return a list of all valid paths
     */
    public List<Path> getSubPaths() {
        List<Path> paths   = new ArrayList();
        Path       subpath = new Path();

        for (int i = 0; i < size(); i++) {
            boolean optional = false;
            String  part     = get(i);
            if (part.startsWith("[")) {
                optional = true;
                part = part.substring(1, part.length() - 1);
            }

            if (optional) {
                paths.add(new Path(subpath));
            }
            subpath.add(part);

            if (isWildcard(i))
                break;
        }
        paths.add(new Path(subpath));

        return paths;
    }



}
