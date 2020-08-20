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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;
import io.inversion.ApiException;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Stream;

/**
 * Yet another JavaScript/JSON map object representation with a few superpowers.
 * <p>
 * Inversion encourages working with JSON data structures abstractly instead of transcoding them into a concrete Java object model.
 * So JSNode and JSArray were designed to make it as easy as possible to work with JSON in its "native form."
 * <p>
 * JSNode and JSArray are a one stop shop for:
 * <ul>
 *  <li>Parsing and printing JSON
 *  <li>Finding elements of a document with JSONPath and JSONPointer
 *  <li>Diff and patching with JSONPatch
 * </ul>
 * <p>
 * Property name case is preserved but considered case insensitive when accessing a property by name.
 * <p>
 * Property iteration or is preserved based on insertion order.
 * <p>
 * It is possible to create a document with a child JSNode appearing multiple times in the document including circular reference loops.
 * When printing a document, if a JSNode has previously been printed AND it has an 'href' property, instead of erroring, the printer
 * will write an '@link' property pointing to the previously printed href.  If the JSNode does not have an 'href' an error will be thrown.
 * <p>
 * Under the covers this Jackson is used as the json parser.
 *
 * @see JSArray
 * @see <a href="https://github.com/json-path/JsonPath">JSONPath</a>
 * @see <a href="https://tools.ietf.org/html/rfc6901">JSONPointer</a>
 * @see <a href="https://github.com/flipkart-incubator/zjsonpatch">JSONPatch</a>
 */
public class JSNode implements Map<String, Object> {
    /**
     * Maps the lower case JSProperty.name to the property for case
     * insensitive lookup with the ability to preserve the original
     * case.
     */
    LinkedHashMap<String, JSProperty> properties = new LinkedHashMap<>();

    /**
     * Creates an empty JSNode.
     */
    public JSNode() {

    }

    /**
     * Creates a JSNode with <code>nameValuePairs</code> as the initial properties.
     * <p>
     * The first and every other element in <code>nameValuePairs</code> should be a string.
     *
     * @param nameValuePairs the name value pairs to add
     * @see #with(Object...)
     */
    public JSNode(Object... nameValuePairs) {
        with(nameValuePairs);
    }

    /**
     * Creates a JSNode with <code>nameValuePairs</code> as the initial properties.
     *
     * @param nameValuePairs the name value pairs to add
     * @see #putAll(Map)
     */
    public JSNode(Map nameValuePairs) {
        putAll(nameValuePairs);
    }

    /**
     * A heroically permissive finder supporting JSON Pointer, JSONPath and
     * a simple 'dot and wildcard' type of system like so:
     * 'propName.childPropName.*.skippedGenerationPropsName.4.fifthArrayNodeChildPropsName.**.recursivelyFoundPropsName'.
     *
     * <p>
     * All forms are internally converted into a 'master' form before processing.  This master
     * simply uses '.' to separate property names and array indexes and uses uses '*' to represent
     * a single level wildcard and '**' to represent a recursive wildcard.  For example:
     * <ul>
     *   <li>'myProp' finds 'myProp' in this node.
     *   <li>'myProp.childProp' finds 'childProp' on 'myProp'
     *   <li>'myArrayProp.2.*' finds all properties of the third element of the 'myArrayProp'
     *   <li>'*.myProp' finds 'myProp' in any of the children of this node.
     *   <li>'**.myProp' finds 'myProp' anywhere in my descendents.
     *   <li>'**.myProp.*.value' finds 'value' as a grandchild anywhere under me.
     *   <li>'**.*' returns every element of the document.
     *   <li>'**.5' gets the 6th element of every array.
     *   <li>'**.book[?(&#064;.isbn)]' finds all books with an isbn
     *   <li>'**.[?(&#064;.author = 'Herman Melville')]' finds all book with author 'Herman Melville'
     * </ul>
     * <p>
     * Arrays indexes are treated just like property names but with integer names.
     * For example "myObject.4.nextProperty" finds "nextProperty" on the 5th element
     * in the "myObject" array.
     *
     * <p>
     * JSON Pointer is the least expressive supported form and uses '/' characters to separate properties.
     * To support JSON Pointer, we simply replace all '/' characters for "." characters before
     * processing.
     *
     * <p>
     * JSON Path is more like XML XPath but uses '.' instead of '/' to separate properties.
     * Technically JSON Path statements are supposed to start with '$.' but that is optional here.
     * The best part about JSON Path is the query filters that let you conditionally select
     * elements.
     * <p>
     * Below is the implementation status of various JSON Path features:
     * <ul>
     *  <li>SUPPORTED $.store.book[*].author                     //the authors of all books in the store
     *  <li>SUPPORTED $..author                                  //all authors
     *  <li>SUPPORTED $.store..price                             //the prices of all books
     *  <li>SUPPORTED $..book[2]                                 //the third book
     *  <li>SUPPORTED $..book[?(@.price@lt;10)]                  //all books priced @lt; 10
     *  <li>SUPPORTED $..[?(@.price@lt;10)]                      //find any node with a price property
     *  <li>SUPPORTED $..[?(@.*.price@lt;10)]                    //find the parent of any node with a price property
     *  <li>SUPPORTED $..book[?(@.author = 'Herman Melville')]   //all books where 'Herman Melville' is the author
     *  <li>SUPPORTED $..*                                       //all members of JSON structure.
     *  <li>TODO      $..book[(@.length-1)]                      //the last book in order
     *  <li>TODO      $..book[-1:]                               //the last book in order
     *  <li>TODO      $..book[0,1]                               //the first two books
     *  <li>TODO      $..book[:2]                                //the first two books
     *  <li>SUPPORTED $..book[?(@.isbn)]                         //find all books with an isbn property
     *  <li>SUPPORTED $..[?(@.isbn)]                             //find any node with an isbn property
     *  <li>SUPPORTED $..[?(@.*.isbn)]                           //find the parent of any node with an isbn property
     *  <li>SUPPORTED $..[?(@.*.*.isbn)]                         //find the grandparent of any node with an isbn property
     *
     *
     * </ul>
     * <p>
     * The JSON Path following boolean comparison operators are supported:
     * <ul>
     *  <li> =
     *  <li>@gt;
     *  <li>@lt;
     *  <li>@gt;=
     *  <li>@lt;=
     *  <li>!=
     * </ul>
     *
     * <p>
     * JsonPath bracket-notation such as  "$['store']['book'][0]['title']"
     * is currently not supported.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6901">JSON Pointer</a>
     * @see <a href="https://goessner.net/articles/JsonPath/">JSON Path</a>
     * @see <a href="https://github.com/json-path/JsonPath">JSON Path</a>
     */
    public JSArray findAll(String pathExpression, int qty) {
        pathExpression = fromJsonPointer(pathExpression);
        pathExpression = fromJsonPath(pathExpression);
        return new JSArray(findAll0(pathExpression, qty));
    }

    List findAll0(String jsonPath, int qty) {
        JSONPathTokenizer tok = new JSONPathTokenizer(//
                "['\"", //openQuoteStr
                "]'\"", //closeQuoteStr
                "]", //breakIncludedChars
                ".", //breakExcludedChars
                "", //unquotedIgnoredChars
                ". \t", //leadingIgnoredChars
                jsonPath //chars
        );

        List<String> path = tok.asList();
        return findAll0(path, qty, new ArrayList<>());
    }

    List findAll0(List<String> path, int qty, List collected) {
        if (qty > 1 && collected.size() >= qty)
            return collected;

        String nextSegment = path.get(0);

        if ("*".equals(nextSegment)) {
            if (path.size() == 1) {
                Collection values = values();

                for (Object value : values) {
                    if (!collected.contains(value) && (qty < 1 || collected.size() < qty))
                        collected.add(value);
                }
            } else {
                List<String> nextPath = path.subList(1, path.size());
                for (Object value : values()) {
                    if (value instanceof JSNode) {
                        ((JSNode) value).findAll0(nextPath, qty, collected);
                    }
                }
            }
        } else if ("**".equals(nextSegment)) {
            if (path.size() != 1) {
                List<String> nextPath = path.subList(1, path.size());
                this.findAll0(nextPath, qty, collected);
                for (Object value : values()) {
                    if (value instanceof JSNode) {
                        ((JSNode) value).findAll0(path, qty, collected);
                    }
                }
            }
        }
        //      else if (this instanceof JSArray && nextSegment.startsWith("[") && nextSegment.endsWith("]"))
        else if (nextSegment.startsWith("[") && nextSegment.endsWith("]")) {
            //this is a JSONPath filter that is not just an array index
            String expr = nextSegment.substring(1, nextSegment.length() - 1).trim();
            if (expr.startsWith("?(") && expr.endsWith(")")) {
                JSONPathTokenizer tokenizer = new JSONPathTokenizer(//
                        "'\"", //openQuoteStr
                        "'\"", //closeQuoteStr
                        "?=<>!", //breakIncludedChars...breakAfter
                        "]=<>! ", //breakExcludedChars...breakBefore
                        "[()", //unquotedIgnoredChars
                        "]. \t", //leadingIgnoredChars
                        expr);

                String token;
                String func    = null;
                String subpath = null;
                String op      = null;
                String value   = null;

                //-- Choices after tokenization
                //-- $..book[2]  -> 2
                //-- $..book[author] -> author
                //-- $..book[(@.length-1)] -> @_length-1
                //-- $..book[-1:] -> -1:
                //-- $..book[0,1] -> 0,1
                //-- $..book[:2] -> :2
                //-- $..book[?(@.isbn)] -> ? @_isbn
                //-- $..book[?(@.price<10)] -> ?


                while ((token = tokenizer.next()) != null) {
                    if (token.equals("?")) {
                        func = "?";
                        continue;
                    }

                    if (token.startsWith("@_")) {
                        subpath = token.substring(2);
                    } else if (Utils.in(token, "=", ">", "<", "!")) {
                        if (op == null)
                            op = token;
                        else
                            op += token;
                    } else if (subpath != null && op != null && value == null) {
                        value = token;

                        if (isArray()) {
                            for (Object child : values()) {
                                if (child instanceof JSNode) {
                                    List found = ((JSNode) child).findAll0(subpath, -1);
                                    for (Object val : found) {
                                        if (eval(val, op, value)) {
                                            if (!collected.contains(child) && (qty < 1 || collected.size() < qty))
                                                collected.add(child);
                                        }
                                    }
                                }
                            }
                        } else {
                            List found = findAll0(subpath, -1);
                            for (Object val : found) {
                                if (eval(val, op, value)) {
                                    if (!collected.contains(this) && (qty < 1 || collected.size() < qty)) {
                                        collected.add(this);
                                        break;
                                    }
                                }
                            }
                        }

                        func = null;
                        subpath = null;
                        op = null;
                        value = null;
                    }
                }
                //$..book[?(@.isbn)] -- checks for the existence of a property
                if ("?".equals(func) && subpath != null) {
                    if (op != null || value != null) {
                        //unparseable...do nothing
                    }

                    if (isArray()) {
                        for (Object child : values()) {
                            if (child instanceof JSNode) {
                                List found = ((JSNode) child).findAll0(subpath, -1);
                                for (Object val : found) {
                                    if (!collected.contains(child) && (qty < 1 || collected.size() < qty))
                                        collected.add(child);
                                }
                            }
                        }
                    } else {
                        List found = findAll0(subpath, -1);
                        if (found.size() > 0) {
                            if (!collected.contains(this) && (qty < 1 || collected.size() < qty))
                                collected.add(this);
                        }
                    }
                }
            }
        } else {
            Object found = null;
            try {
                found = get(nextSegment);
            } catch (NumberFormatException ex) {
                //trying to access an array with a prop name...ignore
            }
            if (found != null) {
                if (path.size() == 1) {
                    if (!collected.contains(found) && (qty < 1 || collected.size() < qty))
                        collected.add(found);
                } else if (found instanceof JSNode) {
                    ((JSNode) found).findAll0(path.subList(1, path.size()), qty, collected);
                }
            }
        }

        return collected;
    }

    boolean eval(Object var, String op, Object value) {
        value = Utils.dequote(value.toString());

        if (var instanceof Number) {
            try {
                value = Double.parseDouble(value.toString());
            } catch (Exception ex) {
                //ok, value was not a number...ignore
            }
        }

        if (var instanceof Boolean) {
            try {
                value = Boolean.parseBoolean(value.toString());
            } catch (Exception ex) {
                //ok, value was not a boolean...ignore
            }
        }

        int comp = ((Comparable) var).compareTo(value);

        switch (op) {
            case "=":
                return comp == 0;
            case ">":
                return comp > 0;
            case ">=":
                return comp >= 0;
            case "<":
                return comp < 0;
            case "<=":
                return comp <= 0;
            case "!=":
                return comp != 0;
            default:
                throw new UnsupportedOperationException("Unknown operator '" + op + "'");
        }
    }

    public JSArray diff(JSNode source) {
        ObjectMapper mapper = new ObjectMapper();

        JsonNode patch;
        try {
            patch = JsonDiff.asJson(mapper.readValue(source.toString(), JsonNode.class), mapper.readValue(this.toString(), JsonNode.class));
            JSArray patchesArray = JSNode.parseJsonArray(patch.toPrettyString());
            return patchesArray;
        } catch (Exception e) {
            e.printStackTrace();
            Utils.rethrow(e);
        }

        return null;
    }

    public JSArray patch(JSArray patches) {
        //-- migrate legacy "." based paths to JSONPointer
        for (JSNode patch : patches.asNodeList()) {
            String path = patch.getString("path");
            if (path != null && !path.startsWith("/")) {
                path = "/" + path.replace(".", "/");
            }
            patch.put("path", path);

            path = patch.getString("from");
            if (path != null && !path.startsWith("/")) {
                path = "/" + path.replace(".", "/");
            }
            patch.put("from", path);
        }

        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode target  = JsonPatch.apply(mapper.readValue(patches.toString(), JsonNode.class), mapper.readValue(this.toString(), JsonNode.class));
            JSNode   patched = JSNode.parseJsonNode(target.toString());

            this.properties = patched.properties;
            if (this.isArray()) {
                ((JSArray) this).objects = ((JSArray) patched).objects;
            }
        } catch (Exception e) {
            Utils.rethrow(e);
        }

        return null;
    }

    JSProperty getProperty(String name) {
        if (name == null)
            return null;

        return this.properties.get(name.toLowerCase());
    }

    @Override
    public Object get(Object name) {
        if (name == null)
            return null;

        JSProperty p = getProperty(name.toString());
        if (p != null)
            return p.getValue();

        return null;
    }

    /**
     * Convenience overloading of {@link #get(Object)}
     *
     * @param name
     * @return the value of property <code>name</code> cast to a JSNode if exists else null
     * @throws ClassCastException if the object found is not a JSNode
     * @see #get(Object)
     */
    public JSNode getNode(String name) throws ClassCastException {
        return (JSNode) get(name);
    }

    /**
     * Convenience overloading of {@link #get(Object)}
     *
     * @param name
     * @return the value of property <code>name</code> cast to a JSArray if exists else null
     * @throws ClassCastException if the object found is not a JSArray
     * @see #get(Object)
     */
    public JSArray getArray(String name) {
        return (JSArray) get(name);
    }

    /**
     * Convenience overloading of {@link #get(Object)}
     *
     * @param name
     * @return the stringified value of property <code>name</code> if it exists else null
     * @see #get(Object)
     */
    public String getString(String name) {
        Object value = get(name);
        if (value != null)
            return value.toString();
        return null;
    }

    /**
     * Convenience overloading of {@link #get(Object)}
     *
     * @param name
     * @return the value of property <code>name</code> stringified and parsed as an int if it exists else -1
     * @see #get(Object)
     */
    public int getInt(String name) {
        Object found = get(name);
        if (found != null)
            return Utils.atoi(found);

        return -1;
    }

    /**
     * Convenience overloading of {@link #get(Object)}
     *
     * @param name
     * @return the value of property <code>name</code> stringified and parsed as a double if it exists else -1
     * @see #get(Object)
     */
    public double getDouble(String name) {
        Object found = get(name);
        if (found != null)
            return Utils.atod(found);

        return -1;
    }

    /**
     * Convenience overloading of {@link #get(Object)}
     *
     * @param name
     * @return the value of property <code>name</code> stringified and parsed as a boolean if it exists else false
     * @see #get(Object)
     */
    public boolean getBoolean(String name) {
        Object found = get(name);
        if (found != null)
            return Utils.atob(found);

        return false;
    }

    /**
     * Convenience overloading of {@link #find(String)}
     *
     * @param pathExpression specifies the nodes to find
     * @return the first value found at <code>pathExpression</code> cast as a JSNode if exists else null
     * @throws ClassCastException if the object found is not a JSNode
     * @see #find(String)
     */
    public JSNode findNode(String pathExpression) {
        return (JSNode) find(pathExpression);
    }

    /**
     * Convenience overloading of {@link #find(String)}
     *
     * @param pathExpression specifies the nodes to find
     * @return the first value found at <code>pathExpression</code> cast as a JSArray if exists else null
     * @throws ClassCastException if the object found is not a JSArray
     * @see #find(String)
     */
    public JSArray findArray(String pathExpression) {
        return (JSArray) find(pathExpression);
    }

    /**
     * Convenience overloading of {@link #find(String)}
     *
     * @param pathExpression
     * @return the first value found at <code>pathExpression</code> stringified if exists else null
     * @see #find(String)
     */
    public String findString(String pathExpression) {
        Object found = find(pathExpression);
        if (found != null)
            return found.toString();

        return null;
    }

    /**
     * Convenience overloading of {@link #find(String)}
     *
     * @param pathExpression
     * @return the first value found at <code>pathExpression</code> stringified and parsed as an int if exists else -1
     * @see #find(String)
     */
    public int findInt(String pathExpression) {
        Object found = find(pathExpression);
        if (found != null)
            return Utils.atoi(found);

        return -1;
    }

    /**
     * Convenience overloading of {@link #find(String)}
     *
     * @param pathExpression
     * @return the first value found at <code>pathExpression</code> stringified and parsed as a double if exists else -1
     * @see #find(String)
     */
    public double findDouble(String pathExpression) {
        Object found = find(pathExpression);
        if (found != null)
            return Utils.atod(found);

        return -1;
    }

    /**
     * Convenience overloading of {@link #find(String)}
     *
     * @param pathExpression
     * @return the first value found at <code>pathExpression</code> stringified and parsed as a boolean if exists else false
     * @see #find(String)
     */
    public boolean findBoolean(String pathExpression) {
        Object found = find(pathExpression);
        if (found != null)
            return Utils.atob(found);

        return false;
    }

    /**
     * Convenience overloading of {@link #findAll(String, int)} that returns the first item found
     *
     * @return the first item found at <code>pathExpression</code>
     * @see #findAll(String, int)
     */
    public Object find(String pathExpression) {
        JSArray found = findAll(pathExpression, 1);
        if (found.size() > 0)
            return found.get(0);

        return null;
    }

    /**
     * Convenience overloading of {@link #findAll(String, int)}
     *
     * @param pathExpression
     * @return all items found for <code>pathExpression</code>
     * @see #findAll(String, int)
     */
    public JSArray findAll(String pathExpression) {
        return findAll(pathExpression, -1);
    }

    /**
     * Convenience overloading of {@link #findAll(String, int)}
     *
     * @param pathExpression
     * @return all items found for <code>pathExpression</code> cast as a List
     * @see #findAll(String, int)
     */
    public List<JSNode> findAllNodes(String pathExpression) {
        @SuppressWarnings({"inconvertable"}) List found = findAll(pathExpression).asList();
        return found;
    }

    @Override
    public Object put(String name, Object value) {
        JSProperty prop = properties.put(name.toLowerCase(), new JSProperty(name, value));
        return prop;
    }

    @Override
    public void putAll(Map map) {
        for (Object key : map.keySet()) {
            put(key.toString(), map.get(key));
        }
    }

    /**
     * Vanity method to make sure the attributes prints out first
     *
     * @param name
     * @param value
     * @return
     */
    public Object putFirst(String name, Object value) {
        LinkedHashMap<String, JSProperty> temp = new LinkedHashMap();

        JSProperty prop = temp.put(name.toLowerCase(), new JSProperty(name, value));

        this.properties.remove(name.toLowerCase());
        temp.putAll(this.properties);
        this.properties = temp;

        return prop;
    }

    public JSNode with(Object... nvPairs) {
        if (nvPairs == null || nvPairs.length == 0)
            return this;

        if (nvPairs.length % 2 != 0)
            throw new RuntimeException("You must supply an even number of arguments to JSNode.with()");

        for (int i = 0; i < nvPairs.length - 1; i += 2) {
            Object value = nvPairs[i + 1];

            if (value instanceof Map && !(value instanceof JSNode))
                throw new RuntimeException("Invalid map value");

            if (value instanceof List && !(value instanceof JSArray))
                throw new RuntimeException("Invalid list value");

            put(nvPairs[i] + "", value);
        }

        return this;
    }

    @Override
    public boolean containsKey(Object name) {
        if (name == null)
            return false;

        return properties.containsKey(name.toString().toLowerCase());
    }

    @Override
    public Object remove(Object name) {
        if (name == null)
            return null;

        JSProperty old = removeProperty(name.toString());
        return old != null ? old.getValue() : old;
    }

    /**
     * Removes all properties with <code>names</code>.
     *
     * @param names the keys to remove
     * @return the first non null value for <code>names</code>
     */
    public Object remove(String... names) {
        Object first = null;

        for (String name : names) {
            Object removed = remove(name);
            first = first != null ? first : removed;
        }
        return first;
    }

    @Override
    public Set<String> keySet() {
        //properties.getKeySet contains the lower case versions.
        LinkedHashSet keys = new LinkedHashSet();
        for (String key : properties.keySet()) {
            JSProperty p = getProperty(key);
            keys.add(p.getName());
        }
        return keys;
    }

    public boolean hasProperty(String name) {
        JSProperty property = getProperty(name);
        return property != null;
    }

    List<JSProperty> getProperties() {
        return new ArrayList(properties.values());
    }

    JSProperty removeProperty(String name) {
        JSProperty property = getProperty(name.toLowerCase());
        if (property != null)
            properties.remove(name.toLowerCase());

        return property;
    }

    static class JSProperty {
        String name  = null;
        Object value = null;

        public JSProperty(String name, Object value) {
            super();
            this.name = name;
            this.value = value;
        }

        public String toString() {
            return name + " = " + value;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @param name the name to set
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return the value
         */
        public Object getValue() {
            return value;
        }

        /**
         * @param value the value to set
         */
        public void setValue(Object value) {
            this.value = value;
        }
    }

    public Map asMap() {
        Map map = new LinkedHashMap();
        for (JSProperty p : properties.values()) {
            String name  = p.name;
            Object value = p.value;
            map.put(name, value);
        }
        return map;
    }

    /**
     * Makes a deep copy of this JSNode by stringifying/parsing.
     *
     * @return a deep copy of this node.
     */
    public JSNode copy() {
        return JSNode.parseJsonNode(toString());
    }

    /**
     * Changes the property name iteration order from insertion order to alphabetic order.
     */
    public void sortKeys() {
        List<String> keys = new ArrayList(properties.keySet());
        Collections.sort(keys);

        LinkedHashMap<String, JSProperty> newProps = new LinkedHashMap();
        for (String key : keys) {
            newProps.put(key, getProperty(key));
        }
        properties = newProps;
    }

    /**
     * Easy alternative to 'instanceof' to differentiate JSNode from JSArray (which subclasses JSNode).
     *
     * @return true if this class is a subclass of JSArray.
     * @see JSArray#isArray()
     */
    public boolean isArray() {
        return false;
    }

    /**
     * Pretty prints the JSNode with properties written out in their original case.
     */
    @Override
    public String toString() {
        return JSNode.toJson((JSNode) this, true, false);
    }

    /**
     * Prints the JSNode with properties written out in their original case.
     *
     * @param pretty should spaces and carriage returns be added to the doc for readability
     */
    public String toString(boolean pretty) {
        return JSNode.toJson((JSNode) this, pretty, false);
    }

    /**
     * Prints the JSNode
     *
     * @param pretty should spaces and carriage returns be added to the doc for readability
     * @param lowercasePropertyNames when true all property names are printed in lower case instead of their original case
     */
    public String toString(boolean pretty, boolean lowercasePropertyNames) {
        return JSNode.toJson((JSNode) this, pretty, lowercasePropertyNames);
    }

    /**
     * @return the number of properties on this node.
     */
    @Override
    public int size() {
        return properties.size();
    }

    /**
     * @return true if <code>size() == 0</code>
     */
    @Override
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    /**
     * Checks all property values for equality to <code>value</code>
     *
     * @return true if any property values are equal to <code>value</code>
     */
    @Override
    public boolean containsValue(Object value) {
        if (value == null)
            return false;

        for (JSProperty prop : properties.values())
            if (value.equals(prop.getValue()))
                return true;

        return false;
    }

    /**
     * Removes all properties
     */
    @Override
    public void clear() {
        properties.clear();
    }

    /**
     * @return a collection of property values
     */
    @Override
    public Collection values() {
        return asMap().values();
    }

    /**
     * @return all property name / value pairs
     */
    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return asMap().entrySet();
    }

    /**
     * Returns this object as the only element in a list.
     * <p>
     * JSArray overrides this method to return all of its elements in a list.
     * <p>
     * This method is designed to make it super easy to iterate over all
     * property values or array elements without having to cast or consider
     * differences between JSNode and JSArray.
     * <p>
     * For example:
     * <p>
     * <pre>
     * JSNode node = response.getJson();//don't know if this is a JSNode or JSArray
     * for(Object value : node.asList())
     * {
     *    //do something;
     * }
     * </pre>
     *
     * @return A List with this node as the only value.
     * @see JSArray#asList()
     * @see #asNodeList()
     */
    public List asList() {
        ArrayList list = new ArrayList<>();
        list.add(this);
        return list;
    }

    /**
     * Returns this object as the only element in a List
     * <p>
     * JSArray overrides this method to return all of its elements in a list.
     * <p>
     * This method is designed to make it super easy to iterate over all
     * property values or array elements without having to cast or consider
     * differences between JSNode and JSArray.
     * <p>
     * For example:
     * <p>
     * <pre>
     * JSNode node = response.getJson();//don't know if this is a JSNode or JSArray
     * for(JSNode child : node.asList())
     * {
     *    System.out.println("found items with price: " + child.find("**.item.price"));
     * }
     * </pre>
     *
     * @return A List with this node as the only value.
     * @see #asList()
     */
    public List<JSNode> asNodeList() {
        @SuppressWarnings({"inconvertable"}) List list = asList();
        return list;
    }

    /**
     * Similar to #asList() but instead of returning a List, it returns a this JSNode
     * as the only item in a JSArray.
     * <p>
     * JSArray overrides this method to simply return 'this'.
     * <p>
     *
     * @return a JSArray with 'this' as the only element.
     * @see #asList()
     * @see #asNodeList()
     * @see JSArray#asArray()
     */
    public JSArray asArray() {
        return new JSArray(this);
    }

    /**
     * Convenience method that calls asList().stream().
     * @return asList().stream()
     */
    public Stream stream()
    {
        return asList().stream();
    }

    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------
    //-- The following methods are static parse/print related

    /**
     * Turns a JSON string in to JSNode (maps), JSArray (lists), String numbers and booleans.
     * <p>
     * Jackson is the underlying parser
     *
     * @param json the json string to parse
     * @return a String, number, boolean, JSNode or JSArray
     */
    public static Object parseJson(String json) {
        try {
            ObjectMapper mapper   = new ObjectMapper();
            JsonNode     rootNode = mapper.readValue(json, JsonNode.class);

            Object parsed = JSNode.mapNode(rootNode);
            return parsed;
        } catch (Exception ex) {
            String msg = "Error parsing JSON:" + ex.getMessage();

            if (!(ex instanceof JsonParseException)) {
                msg += "\r\nSource:" + json;
            }

            throw new RuntimeException("400 Bad Request: '" + msg + "'");
        }
    }

    /**
     * Utility overloading of {@link #parseJson(String)} to cast the return as a JSNode
     *
     * @param json
     * @return the result of parsing the json document cast to a JSNode
     * @throws ClassCastException if the result of parsing is not a JSNode
     */
    public static JSNode parseJsonNode(String json) throws ClassCastException {
        return ((JSNode) JSNode.parseJson(json));
    }

    /**
     * Utility overloading of {@link #parseJson(String)} to cast the return as a JSArray
     *
     * @param json
     * @return the result of parsing the json document cast to a JSArray
     * @throws ClassCastException if the result of parsing is not a JSArray
     */
    public static JSArray parseJsonArray(String json) {
        return ((JSArray) JSNode.parseJson(json));
    }

    static String toJson(JSNode node, boolean pretty, boolean lowercasePropertyNames) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JsonGenerator         json = new JsonFactory().createGenerator(baos);
            if (pretty)
                json.useDefaultPrettyPrinter();

            JSNode.writeNode(node, json, new HashSet(), lowercasePropertyNames);
            json.flush();
            baos.flush();

            return new String(baos.toByteArray());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    static void writeArrayNode(JSArray array, JsonGenerator json, HashSet visited, boolean lowercaseNames) throws Exception {
        json.writeStartArray();
        for (Object obj : array.asList()) {
            if (obj == null) {
                json.writeNull();
            } else if (obj instanceof JSNode) {
                writeNode((JSNode) obj, json, visited, lowercaseNames);
            } else if (obj instanceof BigDecimal) {
                json.writeNumber((BigDecimal) obj);
            } else if (obj instanceof Double) {
                json.writeNumber((Double) obj);
            } else if (obj instanceof Float) {
                json.writeNumber((Float) obj);
            } else if (obj instanceof Integer) {
                json.writeNumber((Integer) obj);
            } else if (obj instanceof Long) {
                json.writeNumber((Long) obj);
            } else if (obj instanceof BigDecimal) {
                json.writeNumber((BigDecimal) obj);
            } else if (obj instanceof BigDecimal) {
                json.writeNumber((BigDecimal) obj);
            } else if (obj instanceof Boolean) {
                json.writeBoolean((Boolean) obj);
            } else {
                json.writeString(encodeStringValue(obj + ""));
            }
        }
        json.writeEndArray();
    }

    static Object mapNode(JsonNode json) {
        if (json == null)
            return null;

        if (json.isNull())
            return null;

        if (json.isValueNode()) {
            if (json.isNumber())
                return json.numberValue();

            if (json.isBoolean())
                return json.booleanValue();

            return json.asText();
        }

        if (json.isArray()) {
            JSArray retVal = null;
            retVal = new JSArray();

            for (JsonNode child : json) {
                retVal.add(mapNode(child));
            }

            return retVal;
        } else if (json.isObject()) {
            JSNode retVal = null;
            retVal = new JSNode();

            Iterator<String> it = json.fieldNames();
            while (it.hasNext()) {
                String   field = it.next();
                JsonNode value = json.get(field);
                retVal.put(field, mapNode(value));
            }
            return retVal;
        }

        throw new RuntimeException("unparseable json:" + json);
    }

    /**
     * Replaces JSON control characters with spaces.
     *
     * @param str
     * @return str with control characters replaced with spaces
     * @see <a href="https://stackoverflow.com/questions/14028716/how-to-remove-control-characters-from-java-string">How to remove control characters from java Strings</a>
     */
    static String encodeStringValue(String str) {
        if (str == null)
            return null;

        str = str.replaceAll("[\\p{Cntrl}\\p{Cc}\\p{Cf}\\p{Co}\\p{Cn}\u00A0&&[^\r\n\t]]", " ");
        return str;
    }

    static void writeNode(JSNode node, JsonGenerator json, HashSet visited, boolean lowercaseNames) throws Exception {
        JSProperty href = node.getProperty("href");

        if (visited.contains(node)) {
            if (href != null) {
                json.writeStartObject();
                if (href != null) {
                    json.writeStringField("@link", href.getValue() + "");
                }

                json.writeEndObject();
            } else {
                throw ApiException.new500InternalServerError("Your JSNode document contains the same object in multiple locations without a 'href' property.");
            }
            return;
        }
        visited.add(node);

        if (node instanceof JSArray) {
            JSNode.writeArrayNode(((JSArray) node), json, visited, lowercaseNames);
            return;
        }

        json.writeStartObject();

        if (href != null)
            json.writeStringField("href", href.getValue() + "");

        for (String key : node.keySet()) {
            JSProperty p = node.getProperty(key);
            if (p == href)
                continue;

            String name  = p.getName();
            Object value = p.getValue();

            if (value == null) {
                json.writeNullField(name);
            } else if (value instanceof JSNode) {
                if (!lowercaseNames)
                    json.writeFieldName(name);
                else
                    json.writeFieldName(name.toLowerCase());

                writeNode((JSNode) value, json, visited, lowercaseNames);
            } else if (value instanceof Date) {
                json.writeStringField(name, Utils.formatDate((Date) value, "yyyy-MM-dd'T'HH:mmZ"));
            } else if (value instanceof BigDecimal) {
                json.writeNumberField(name, (BigDecimal) value);
            } else if (value instanceof Double) {
                json.writeNumberField(name, (Double) value);
            } else if (value instanceof Float) {
                json.writeNumberField(name, (Float) value);
            } else if (value instanceof Integer) {
                json.writeNumberField(name, (Integer) value);
            } else if (value instanceof Long) {
                json.writeNumberField(name, (Long) value);
            } else if (value instanceof BigDecimal) {
                json.writeNumberField(name, (BigDecimal) value);
            } else if (value instanceof BigInteger) {
                json.writeNumberField(name, ((BigInteger) value).intValue());
            } else if (value instanceof Boolean) {
                json.writeBooleanField(name, (Boolean) value);
            } else {
                String strVal = value + "";
                if ("null".equals(strVal)) {
                    json.writeNullField(name);
                } else {
                    strVal = JSNode.encodeStringValue(strVal);
                    json.writeStringField(name, strVal);
                }
            }
        }
        json.writeEndObject();
    }

    /**
     * Simply replaces "/"s with "."
     * <p>
     * Slashes in property names (seriously a stupid idea anyway) which is supported
     * by JSON Pointer is not supported.
     *
     * @param jsonPointer
     * @return
     */
    static String fromJsonPointer(String jsonPointer) {
        return jsonPointer.replace('/', '.');
    }

    /**
     * Converts a proper json path statement into its "relaxed dotted wildcard" form
     * so that it is easier to parse.
     */
    static String fromJsonPath(String jsonPath) {
        if (jsonPath.charAt(0) == '$')
            jsonPath = jsonPath.substring(1, jsonPath.length());

        jsonPath = jsonPath.replace("@.", "@_"); //from jsonpath spec..switching to "_" to make parsing easier
        jsonPath = jsonPath.replaceAll("([a-zA-Z])\\[", "$1.["); //from json path spec array[index] converted to array.[index]. to support array.index.value legacy format.
        jsonPath = jsonPath.replace("..", "**."); //translate from jsonpath format
        jsonPath = jsonPath.replaceAll("([a-zA-Z])[*]", "$1.*"); //translate from jsonpath format
        jsonPath = jsonPath.replaceAll("([a-zA-Z])\\[([0-9]*)\\]", "$1.$2"); // x[1] to x.1
        jsonPath = jsonPath.replaceAll("\\.\\[([0-9]*)\\]", ".$1"); //translate .[1]. to .1. */
        jsonPath = jsonPath.replace("[*]", "*");

        //System.out.println(pathStr);
        return jsonPath;
    }

    static class JSONPathTokenizer {
        char[] chars = null;
        int    head  = 0;

        char    escapeChar = '\\';
        boolean escaped    = false;
        boolean quoted     = false;

        StringBuilder next = new StringBuilder();

        Set openQuotes      = null;
        Set closeQuotes     = null;
        Set breakIncluded   = null;
        Set breakExcluded   = null;
        Set unquotedIgnored = null;
        Set leadingIgnored  = null;

        public JSONPathTokenizer(String openQuoteChars, String closeQuoteChars, String breakIncludedChars, String breakExcludedChars, String unquotedIgnoredChars, String leadingIgnoredChars) {
            this(openQuoteChars, closeQuoteChars, breakIncludedChars, breakExcludedChars, unquotedIgnoredChars, leadingIgnoredChars, null);
        }

        public JSONPathTokenizer(String openQuoteChars, String closeQuoteChars, String breakIncludedChars, String breakExcludedChars, String unquotedIgnoredChars, String leadingIgnoredChars, String chars) {
            openQuotes = toSet(openQuoteChars);
            closeQuotes = toSet(closeQuoteChars);
            breakIncluded = toSet(breakIncludedChars);
            breakExcluded = toSet(breakExcludedChars);
            unquotedIgnored = toSet(unquotedIgnoredChars);
            leadingIgnored = toSet(leadingIgnoredChars);

            withChars(chars);
        }

        /**
         * Resets any ongoing tokenization to tokenize this new string;
         *
         * @param chars
         */
        public JSONPathTokenizer withChars(String chars) {
            if (chars != null) {
                this.chars = chars.toCharArray();
            }
            head = 0;
            next = new StringBuilder();
            escaped = false;
            quoted = false;

            return this;
        }

        public List<String> asList() {
            List<String> list = new ArrayList<>();
            String       next = null;
            while ((next = next()) != null)
                list.add(next);

            return list;
        }

        Set toSet(String string) {
            Set resultSet = new HashSet();
            for (int i = 0; i < string.length(); i++)
                resultSet.add(new Character(string.charAt(i)));

            return resultSet;
        }

        public String next() {
            if (head >= chars.length) {
                return null;
            }

            while (head < chars.length) {
                char c = chars[head];
                head += 1;

                //System.out.println("c = '" + c + "'");

                if (next.length() == 0 && leadingIgnored.contains(c))
                    continue;

                if (c == escapeChar) {
                    if (escaped)
                        append(c);

                    escaped = !escaped;
                    continue;
                }

                if (!quoted && unquotedIgnored.contains(c)) {
                    continue;
                }

                if (!quoted && !escaped && openQuotes.contains(c)) {
                    quoted = true;
                } else if (quoted && !escaped && closeQuotes.contains(c)) {
                    quoted = false;
                }

                if (!quoted && breakExcluded.contains(c) && next.length() > 0) {
                    head--;
                    break;
                }

                if (!quoted && breakIncluded.contains(c)) {
                    append(c);
                    break;
                }

                append(c);
            }

            if (quoted)
                throw new RuntimeException("Unable to parse unterminated quoted string: \"" + String.valueOf(chars) + "\": -> '" + new String(chars) + "'");

            if (escaped)
                throw new RuntimeException("Unable to parse hanging escape character: \"" + String.valueOf(chars) + "\": -> '" + new String(chars) + "'");

            String str = next.toString().trim();
            next = new StringBuilder();

            if (str.length() == 0)
                str = null;

            return str;
        }

        void append(char c) {
            next.append(c);
        }

    }

}
