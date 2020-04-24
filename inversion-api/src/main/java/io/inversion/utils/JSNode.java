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

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;

/**
 * 
 * TODO: 
 *  Replace diff/patch with open source versions 
 *   https://stackoverflow.com/questions/50967015/how-to-compare-json-documents-and-return-the-differences-with-jackson-or-gson
 *   https://github.com/flipkart-incubator/zjsonpatch
 *   https://github.com/java-json-tools/json-patch
 *   https://javaee.github.io/javaee-spec/javadocs/javax/json/JsonPatch.html
 *   
 * TODO: Investigate MergePatch  
 * 
 *
 */
public class JSNode implements Map<String, Object>
{
   LinkedHashMap<String, JSProperty> properties = new LinkedHashMap();

   public JSNode()
   {

   }

   public JSNode(Object... nvPairs)
   {
      with(nvPairs);
   }

   public JSNode(Map map)
   {
      putAll(map);
   }

   public JSNode copy()
   {
      return JSNode.parseJsonNode(toString());
   }

   public void sortKeys()
   {
      List<String> keys = new ArrayList(properties.keySet());
      Collections.sort(keys);

      LinkedHashMap<String, JSProperty> newProps = new LinkedHashMap();
      for (String key : keys)
      {
         newProps.put(key, properties.get(key));
      }
      properties = newProps;
   }

   public JSArray diff(JSNode source)
   {
      ObjectMapper mapper = new ObjectMapper();

      JsonNode patch;
      try
      {
         patch = JsonDiff.asJson(mapper.readValue(source.toString(), JsonNode.class), mapper.readValue(this.toString(), JsonNode.class));
         JSArray patchesArray = JSNode.parseJsonArray(patch.toPrettyString());
         return patchesArray;
      }
      catch (Exception e)
      {
         e.printStackTrace();
         Utils.rethrow(e);
      }

      return null;
   }

   public JSArray patch(JSArray patches)
   {
      //-- migrate legacy "." based paths to JSONPointer
      for (JSNode patch : patches.asNodeList())
      {
         String path = patch.getString("path");
         if (path != null && !path.startsWith("/"))
         {
            path = "/" + path.replace(".", "/");
         }
         patch.put("path", path);

         path = patch.getString("from");
         if (path != null && !path.startsWith("/"))
         {
            path = "/" + path.replace(".", "/");
         }
         patch.put("from", path);
      }

      ObjectMapper mapper = new ObjectMapper();

      try
      {
         JsonNode target = JsonPatch.apply(mapper.readValue(patches.toString(), JsonNode.class), mapper.readValue(this.toString(), JsonNode.class));
         JSNode patched = JSNode.parseJsonNode(target.toString());

         this.properties = patched.properties;
         if (this.isArray())
         {
            ((JSArray) this).objects = ((JSArray) patched).objects;
         }
      }
      catch (Exception e)
      {
         Utils.rethrow(e);
      }

      return null;
   }

   public boolean isArray()
   {
      return false;
   }

   public JSNode getNode(String name)
   {
      return (JSNode) get(name);
   }

   public JSArray getArray(String name)
   {
      return (JSArray) get(name);
   }

   public String getString(String name)
   {
      Object value = get(name);
      if (value != null)
         return value.toString();
      return null;
   }

   public int getInt(String name)
   {
      return findInt(name);
   }

   public double getDouble(String name)
   {
      return findDouble(name);
   }

   public boolean getBoolean(String name)
   {
      return findBoolean(name);
   }

   public String findString(String path)
   {
      Object found = find(path);
      if (found != null)
         return found.toString();

      return null;
   }

   public int findInt(String path)
   {
      Object found = find(path);
      if (found != null)
         return Utils.atoi(found);

      return -1;
   }

   public double findDouble(String path)
   {
      Object found = find(path);
      if (found != null)
         return Utils.atod(found);

      return -1;
   }

   public boolean findBoolean(String path)
   {
      Object found = find(path);
      if (found != null)
         return Utils.atob(found);

      return false;
   }

   public JSNode findNode(String path)
   {
      return (JSNode) find(path);
   }

   public JSArray findArray(String path)
   {
      return (JSArray) find(path);
   }

   /**
    * Calls collect(jsonPath, 1) and returns
    * the first element of the response JSArray
    * or returns null it nothing was found. 
    * 
    * @see collect(jsonPath, qty);
    * @param jsonPath
    * @return
    */
   public Object find(String jsonPath)
   {
      JSArray found = findAll(jsonPath, 1);
      if (found.size() > 0)
         return found.get(0);

      return null;
   }

   /**
    * @deprecated Use {@link #findAllNodes()} instead.
    */
   public List<JSNode> collectNodes(String jsonPath)
   {
      return (List<JSNode>) findAll(jsonPath).asList();
   }

   public JSArray findAll(String jsonPath)
   {
      return findAll(jsonPath, -1);
   }

   public List<JSNode> findAllNodes(String jsonPath)
   {
      return (List<JSNode>) findAll(jsonPath).asList();
   }

   /**
    * A heroically permissive node finder supporting JSON Pointer, JSON Path and
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
    *   <li>'**.book[?(@.isbn)]' finds all books with an isbn
    *   <il>'**.[?(@.author = 'Herman Melville')]' fins all book with author 'Herman Melville'
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
    * 
    * Below is the implementation status of various JSON Path features:
    * <ul>
    *  <li>SUPPORTED $.store.book[*].author                     //the authors of all books in the store
    *  <li>SUPPORTED $..author                                  //all authors
    *  <li>SUPPORTED $.store..price                             //the prices of all books
    *  <li>SUPPORTED $..book[2]                                 //the third book
    *  <li>SUPPORTED $..book[?(@.price<10)]                     //all books priced < 10
    *  <li>SUPPORTED $..book[?(@.author = 'Herman Melville')]   //all books where 'Herman Melville' is the author
    *  <li>SUPPORTED $..*                                       //all members of JSON structure.
    *  <li>TODO      $..book[(@.length-1)]                      //the last book in order
    *  <li>TODO      $..book[-1:]                               //the last book in order
    *  <li>TODO      $..book[0,1]                               //the first two books
    *  <li>TODO      $..book[:2]                                //the first two books
    *  <li>SUPPORTED $..book[?(@.isbn)]                         //filter all books with isbn number
    * </ul>
    * 
    * The JSON Path following boolean comparison operators are supported: 
    * <ul>
    *  <li> =
    *  <li>>
    *  <li><
    *  <li>>=
    *  <li><=
    *  <li>!=
    * </ul>
    * 
    * <p>
    * JsonPath bracket-notation such as  "$['store']['book'][0]['title']"
    * is currently not supported.
    * 
    * @see JSON Pointer - https://tools.ietf.org/html/rfc6901
    * @see JSON Path - https://goessner.net/articles/JsonPath/
    * @see JSON Path - https://github.com/json-path/JsonPath
    */
   public JSArray findAll(String pathExpression, int qty)
   {
      pathExpression = fromJsonPointer(pathExpression);
      pathExpression = fromJsonPath(pathExpression);
      return new JSArray(collect0(pathExpression, qty));
   }

   /**
    * Simply replaces "/"s with "."
    * 
    * Slashes in property names (seriously a stupid idea anyway) which is supported
    * by JSON Pointer is not supported.
    * 
    * @param jsonPointer
    * @return
    */
   protected static String fromJsonPointer(String jsonPointer)
   {
      return jsonPointer.replace('/', '.');
   }

   /**
    * Converts a proper json path statement into its "relaxed dotted wildcard" form
    * so that it is easier to parse.
    */
   protected static String fromJsonPath(String jsonPath)
   {
      if (jsonPath.charAt(0) == '$')
         jsonPath = jsonPath.substring(1, jsonPath.length());

      jsonPath = jsonPath.replace("@.", "@_"); //from jsonpath spec..switching to "_" to make parsing easier
      jsonPath = jsonPath.replaceAll("([a-zA-Z])\\[", "$1.["); //from json path spec array[index] converted to array.[index]. to support arra.index.value legacy format.
      jsonPath = jsonPath.replace("..", "**."); //translate from jsonpath format
      jsonPath = jsonPath.replaceAll("([a-zA-Z])[*]", "$1.*"); //translate from jsonpath format
      jsonPath = jsonPath.replaceAll("([a-zA-Z])\\[([0-9]*)\\]", "$1.$2"); // x[1] to x.1
      jsonPath = jsonPath.replaceAll("\\.\\[([0-9]*)\\]", ".$1"); //translate .[1]. to .1. */
      jsonPath = jsonPath.replace("[*]", "*");

      //System.out.println(pathStr);
      return jsonPath;
   }

   protected List collect0(String jsonPath, int qty)
   {
      JSONPathTokenizer tok = new JSONPathTokenizer(//
                                                    "['\"", //openQuoteStr
                                                    "]'\"", //closeQuoteStr
                                                    "]", //breakIncludedChars
                                                    ".", //breakExcludedChars
                                                    "", //unquuotedIgnoredChars
                                                    ". \t", //leadingIgoredChars
                                                    jsonPath //chars
      );

      List<String> path = tok.asList();
      return collect0(path, qty, new ArrayList());
   }

   protected List collect0(List<String> path, int qty, List collected)
   {
      if (qty > 1 && collected.size() >= qty)
         return collected;

      String nextSegment = path.get(0);

      if ("*".equals(nextSegment))
      {
         if (path.size() == 1)
         {
            Collection values = values();

            for (Object value : values)
            {
               if (qty < 1 || collected.size() < qty)
                  collected.add(value);
            }
         }
         else
         {
            List<String> nextPath = path.subList(1, path.size());
            for (Object value : values())
            {
               if (value instanceof JSNode)
               {
                  ((JSNode) value).collect0(nextPath, qty, collected);
               }
            }
         }
      }
      else if ("**".equals(nextSegment))
      {
         if (path.size() == 1)
         {
            //** does not collect anything.  **/* would collect everything
         }
         else
         {
            List<String> nextPath = path.subList(1, path.size());

            this.collect0(nextPath, qty, collected);

            for (Object value : values())
            {
               if (value instanceof JSNode)
               {
                  ((JSNode) value).collect0(path, qty, collected);
               }
            }
         }
      }
      //      else if (this instanceof JSArray && nextSegment.startsWith("[") && nextSegment.endsWith("]"))
      else if (nextSegment.startsWith("[") && nextSegment.endsWith("]"))
      {
         //this is a JSONPath filter that is not just an array index
         String expr = nextSegment.substring(1, nextSegment.length() - 1).trim();
         if (expr.startsWith("?(") && expr.endsWith(")"))
         {
            JSONPathTokenizer tokenizer = new JSONPathTokenizer(//
                                                                "'\"", //openQuoteStr
                                                                "'\"", //closeQuoteStr
                                                                "?=<>!", //breakIncludedChars...breakAfter
                                                                "]=<>! ", //breakExcludedChars...breakBefore
                                                                "[()", //unquuotedIgnoredChars
                                                                "]. \t", //leadingIgoredChars
                                                                expr);

            String token = null;
            String func = null;
            String subpath = null;
            String op = null;
            String value = null;

            //-- Choices after tokenization
            //-- $..book[2]  -> 2
            //-- $..book[author] -> author
            //-- $..book[(@.length-1)] -> @_length-1
            //-- $..book[-1:] -> -1:
            //-- $..book[0,1] -> 0,1
            //-- $..book[:2] -> :2
            //-- $..book[?(@.isbn)] -> ? @_isbn
            //-- $..book[?(@.price<10)] -> ?

            while ((token = tokenizer.next()) != null)
            {
               if (token.equals("?"))
               {
                  func = "?";
                  continue;
               }

               if (token.startsWith("@_"))
               {
                  subpath = token.substring(2);
               }
               else if (Utils.in(token, "=", ">", "<", "!"))
               {
                  if (op == null)
                     op = token;
                  else
                     op += token;
               }
               else if (subpath != null && op != null && value == null)
               {
                  value = token;

                  for (Object child : values())
                  {
                     if (child instanceof JSNode)
                     {
                        List found = ((JSNode) child).collect0(subpath, -1);
                        for (Object val : found)
                        {
                           if (eval(val, op, value))
                           {
                              if (qty < 1 || collected.size() < qty)
                                 collected.add(child);
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
            if ("?".equals(func) && subpath != null)
            {
               if (op != null || value != null)
               {
                  //unparseable...do nothing
               }

               for (Object child : values())
               {
                  if (child instanceof JSNode)
                  {
                     List found = ((JSNode) child).collect0(subpath, -1);
                     for (Object val : found)
                     {
                        if (qty < 1 || collected.size() < qty)
                           collected.add(child);
                     }
                  }
               }

            }

         }
      }
      else
      {
         Object found = null;
         try
         {
            found = get(nextSegment);
         }
         catch (NumberFormatException ex)
         {
            //trying to access an array with a prop name...ignore
         }
         if (found != null)
         {
            if (path.size() == 1)
            {
               if (qty < 1 || collected.size() < qty)
                  collected.add(found);
            }
            else if (found instanceof JSNode)
            {
               ((JSNode) found).collect0(path.subList(1, path.size()), qty, collected);
            }
         }
      }

      return collected;
   }

   boolean eval(Object var, String op, Object value)
   {
      value = Utils.dequote(value.toString());

      if (var instanceof Number)
      {
         try
         {
            value = Double.parseDouble(value.toString());
         }
         catch (Exception ex)
         {
            //ok, value was not a number...ignore
         }
      }

      if (var instanceof Boolean)
      {
         try
         {
            value = Boolean.parseBoolean(value.toString());
         }
         catch (Exception ex)
         {
            //ok, value was not a boolean...ignore
         }
      }

      int comp = ((Comparable) var).compareTo(value);

      switch (op)
      {
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
         default :
            throw new UnsupportedOperationException("Unknown operator '" + op + "'");
      }
   }

   @Override
   public Object get(Object name)
   {
      if (name == null)
         return null;

      JSProperty p = getProperty(name.toString());
      if (p != null)
         return p.getValue();

      return null;
   }

   /**
    * Vanity method to make sure the attributes prints out first
    *
    * @param name
    * @param value
    * @return
    */
   public Object putFirst(String name, Object value)
   {
      LinkedHashMap<String, JSProperty> temp = new LinkedHashMap();

      JSProperty prop = temp.put(name.toLowerCase(), new JSProperty(name, value));

      this.properties.remove(name.toLowerCase());
      temp.putAll(this.properties);
      this.properties = temp;

      return prop;
   }

   @Override
   public Object put(String name, Object value)
   {
      JSProperty prop = properties.put(name.toLowerCase(), new JSProperty(name, value));
      return prop;
   }

   @Override
   public void putAll(Map map)
   {
      for (Object key : map.keySet())
      {
         put(key.toString(), map.get(key));
      }
   }

   public JSNode with(Object... nvPairs)
   {
      if (nvPairs == null || nvPairs.length == 0)
         return this;

      if (nvPairs.length % 2 != 0)
         throw new RuntimeException("You must supply an even number of arguments to JSNode.with()");

      for (int i = 0; i < nvPairs.length - 1; i += 2)
      {
         Object value = nvPairs[i + 1];

         if (value instanceof Map && !(value instanceof JSNode))
            throw new RuntimeException("Invalid map value");

         if (value instanceof List && !(value instanceof JSArray))
            throw new RuntimeException("Invalid list value");

         put(nvPairs[i] + "", value);
      }

      return this;
   }

   JSProperty getProperty(String name)
   {
      return properties.get(name.toLowerCase());
   }

   @Override
   public boolean containsKey(Object name)
   {
      if (name == null)
         return false;

      return properties.containsKey(name.toString().toLowerCase());
   }

   @Override
   public Object remove(Object name)
   {
      if (name == null)
         return null;

      JSProperty old = removeProperty(name.toString());
      return old != null ? old.getValue() : old;
   }

   @Override
   public Set<String> keySet()
   {
      //properties.getKeySet contains the lower case versions.
      LinkedHashSet keys = new LinkedHashSet();
      for (String key : properties.keySet())
      {
         JSProperty p = properties.get(key);
         keys.add(p.getName());
      }
      return keys;
   }

   public boolean hasProperty(String name)
   {
      JSProperty property = properties.get(name.toLowerCase());
      return property != null;
   }

   List<JSProperty> getProperties()
   {
      return new ArrayList(properties.values());
   }

   JSProperty removeProperty(String name)
   {
      JSProperty property = properties.get(name.toLowerCase());
      if (property != null)
         properties.remove(name.toLowerCase());

      return property;
   }

   static class JSProperty
   {
      String name  = null;
      Object value = null;

      public JSProperty(String name, Object value)
      {
         super();
         this.name = name;
         this.value = value;
      }

      public String toString()
      {
         return name + " = " + value;
      }

      /**
       * @return the name
       */
      public String getName()
      {
         return name;
      }

      /**
       * @param name the name to set
       */
      public void setName(String name)
      {
         this.name = name;
      }

      /**
       * @return the value
       */
      public Object getValue()
      {
         return value;
      }

      /**
       * @param value the value to set
       */
      public void setValue(Object value)
      {
         this.value = value;
      }
   }

   public Map asMap()
   {
      Map map = new LinkedHashMap();
      for (JSProperty p : properties.values())
      {
         String name = p.name;
         Object value = p.value;

         if (value instanceof JSArray)
         {
            //map.put(name, ((JSArray) p.getValue()).asList());
            map.put(name, value);
         }
         else
         {

            map.put(name, value);
         }
      }
      return map;
   }

   @Override
   public String toString()
   {
      return JSNode.toJson((JSNode) this, true, false);
   }

   public String toString(boolean pretty)
   {
      return JSNode.toJson((JSNode) this, pretty, false);
   }

   public String toString(boolean pretty, boolean tolowercase)
   {
      return JSNode.toJson((JSNode) this, pretty, tolowercase);
   }

   @Override
   public int size()
   {
      return properties.size();
   }

   @Override
   public boolean isEmpty()
   {
      return properties.isEmpty();
   }

   @Override
   public boolean containsValue(Object value)
   {
      if (value == null)
         return false;

      for (JSProperty prop : properties.values())
         if (value.equals(prop.getValue()))
            return true;

      return false;
   }

   @Override
   public void clear()
   {
      properties.clear();
   }

   @Override
   public Collection values()
   {
      return asMap().values();
   }

   @Override
   public Set entrySet()
   {
      return asMap().entrySet();
   }

   public List asList()
   {
      if (this instanceof JSArray)
         return new ArrayList(((JSArray) this).objects);

      ArrayList list = new ArrayList();
      list.add(this);
      return list;
   }

   public List<JSNode> asNodeList()
   {
      return asList();
   }

   public JSArray asArray()
   {
      if (this instanceof JSArray)
         return (JSArray) this;

      return new JSArray(this);
   }

   //--------------------------------------------------------------------------------------
   //--------------------------------------------------------------------------------------
   //--------------------------------------------------------------------------------------
   //-- The following methods are static parse/print related

   public static Object parseJson(String json)
   {
      try
      {
         ObjectMapper mapper = new ObjectMapper();
         JsonNode rootNode = mapper.readValue(json, JsonNode.class);

         Object parsed = JSNode.mapNode(rootNode);
         return parsed;
      }
      catch (Exception ex)
      {
         String msg = "Error parsing JSON:" + ex.getMessage();

         if (!(ex instanceof JsonParseException))
         {
            msg += "\r\nSource:" + json;
         }

         throw new RuntimeException("400 Bad Request: '" + msg + "'");
      }
   }

   public static JSNode parseJsonNode(String json)
   {
      return ((JSNode) JSNode.parseJson(json));
   }

   public static JSArray parseJsonArray(String json)
   {
      return ((JSArray) JSNode.parseJson(json));
   }

   static String toJson(JSNode node, boolean pretty, boolean lowercaseNames)
   {
      try
      {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         JsonGenerator json = new JsonFactory().createGenerator(baos);
         if (pretty)
            json.useDefaultPrettyPrinter();

         JSNode.writeNode(node, json, new HashSet(), lowercaseNames);
         json.flush();
         baos.flush();

         return new String(baos.toByteArray());
      }
      catch (Exception ex)
      {
         throw new RuntimeException(ex);
      }
   }

   static void writeArrayNode(JSArray array, JsonGenerator json, HashSet visited, boolean lowercaseNames) throws Exception
   {
      json.writeStartArray();
      for (Object obj : array.asList())
      {
         if (obj == null)
         {
            json.writeNull();
         }
         else if (obj instanceof JSNode)
         {
            writeNode((JSNode) obj, json, visited, lowercaseNames);
         }
         else if (obj instanceof BigDecimal)
         {
            json.writeNumber((BigDecimal) obj);
         }
         else if (obj instanceof Double)
         {
            json.writeNumber((Double) obj);
         }
         else if (obj instanceof Float)
         {
            json.writeNumber((Float) obj);
         }
         else if (obj instanceof Integer)
         {
            json.writeNumber((Integer) obj);
         }
         else if (obj instanceof Long)
         {
            json.writeNumber((Long) obj);
         }
         else if (obj instanceof BigDecimal)
         {
            json.writeNumber((BigDecimal) obj);
         }
         else if (obj instanceof BigDecimal)
         {
            json.writeNumber((BigDecimal) obj);
         }
         else if (obj instanceof Boolean)
         {
            json.writeBoolean((Boolean) obj);
         }
         else
         {
            json.writeString(encodeJson(obj + ""));
         }
      }
      json.writeEndArray();
   }

   static Object mapNode(JsonNode json)
   {
      if (json == null)
         return null;

      if (json.isNull())
         return null;

      if (json.isValueNode())
      {
         if (json.isNumber())
            return json.numberValue();

         if (json.isBoolean())
            return json.booleanValue();

         return json.asText();
      }

      if (json.isArray())
      {
         JSArray retVal = null;
         retVal = new JSArray();

         for (JsonNode child : json)
         {
            retVal.add(mapNode(child));
         }

         return retVal;
      }
      else if (json.isObject())
      {
         JSNode retVal = null;
         retVal = new JSNode();

         Iterator<String> it = json.fieldNames();
         while (it.hasNext())
         {
            String field = it.next();
            JsonNode value = json.get(field);
            retVal.put(field, mapNode(value));
         }
         return retVal;
      }

      throw new RuntimeException("unparsable json:" + json);
   }

   /**
    * @see https://stackoverflow.com/questions/14028716/how-to-remove-control-characters-from-java-string
    * @param str
    * @return
    */
   static String encodeJson(String str)
   {
      if (str == null)
         return null;

      str = str.replaceAll("[\\p{Cntrl}\\p{Cc}\\p{Cf}\\p{Co}\\p{Cn}\u00A0&&[^\r\n\t]]", " ");
      return str;
   }

   static void writeNode(JSNode node, JsonGenerator json, HashSet visited, boolean lowercaseNames) throws Exception
   {
      JSProperty href = node.getProperty("href");

      if (visited.contains(node))
      {
         json.writeStartObject();
         if (href != null)
         {
            json.writeStringField("@link", href.getValue() + "");
         }

         json.writeEndObject();
         return;
      }
      visited.add(node);

      if (node instanceof JSArray)
      {
         JSNode.writeArrayNode(((JSArray) node), json, visited, lowercaseNames);
         return;
      }

      json.writeStartObject();

      if (href != null)
         json.writeStringField("href", href.getValue() + "");

      for (String key : node.keySet())
      {
         JSProperty p = node.getProperty(key);
         if (p == href)
            continue;

         String name = p.getName();
         Object value = p.getValue();

         if (value == null)
         {
            json.writeNullField(name);
         }
         else if (value instanceof JSNode)
         {
            if (!lowercaseNames)
               json.writeFieldName(name);
            else
               json.writeFieldName(name.toLowerCase());

            writeNode((JSNode) value, json, visited, lowercaseNames);
         }
         else if (value instanceof Date)
         {
            json.writeStringField(name, Utils.formatDate((Date) value, "yyyy-MM-dd'T'HH:mmZ"));
         }
         else if (value instanceof BigDecimal)
         {
            json.writeNumberField(name, (BigDecimal) value);
         }
         else if (value instanceof Double)
         {
            json.writeNumberField(name, (Double) value);
         }
         else if (value instanceof Float)
         {
            json.writeNumberField(name, (Float) value);
         }
         else if (value instanceof Integer)
         {
            json.writeNumberField(name, (Integer) value);
         }
         else if (value instanceof Long)
         {
            json.writeNumberField(name, (Long) value);
         }
         else if (value instanceof BigDecimal)
         {
            json.writeNumberField(name, (BigDecimal) value);
         }
         else if (value instanceof BigInteger)
         {
            json.writeNumberField(name, ((BigInteger) value).intValue());
         }
         else if (value instanceof Boolean)
         {
            json.writeBooleanField(name, (Boolean) value);
         }
         else
         {
            String strVal = value + "";
            if ("null".equals(strVal))
            {
               json.writeNullField(name);
            }
            else
            {
               strVal = JSNode.encodeJson(strVal);
               json.writeStringField(name, strVal);
            }
         }
      }
      json.writeEndObject();
   }

   /**
    * Removes all empty objects from the tree
    * @param parent - parent node
    */
   private boolean prune0(Object parent)
   {
      if (parent instanceof JSArray)
      {
         JSArray arr = ((JSArray) parent);
         for (int i = 0; i < arr.length(); i++)
         {
            if (prune0(arr.get(i)))
            {
               arr.remove(i);
               i--;
            }
         }
         return arr.length() == 0;
      }
      else if (parent instanceof JSNode)
      {
         boolean prune = true;
         JSNode js = (JSNode) parent;
         for (String key : js.keySet())
         {
            Object child = js.get(key);
            prune &= prune0(child);
         }

         if (prune)
         {
            for (String key : js.keySet())
            {
               js.remove(key);
            }
         }

         return prune;
      }
      else
      {
         return parent == null;
      }
   }

   /**
    * Removes all empty objects from the tree
    * of current JSNode
    */
   public boolean prune()
   {
      return prune0(this);
   }

   static class JSONPathTokenizer
   {
      char[]       chars           = null;
      int          head            = 0;

      char         escapeChar      = '\\';
      boolean      escaped         = false;
      boolean      quoted          = false;

      StringBuffer next            = new StringBuffer();

      Set          openQuotes      = null;
      Set          closeQuotes     = null;
      Set          breakIncluded   = null;
      Set          breakExcluded   = null;
      Set          unquotedIgnored = null;
      Set          leadingIgnored  = null;

      public JSONPathTokenizer(String openQuoteChars, String closeQuoteChars, String breakIncludedChars, String breakExcludedChars, String unquotedIgnoredChars, String leadingIgnoredChars)
      {
         this(openQuoteChars, closeQuoteChars, breakIncludedChars, breakExcludedChars, unquotedIgnoredChars, leadingIgnoredChars, null);
      }

      public JSONPathTokenizer(String openQuoteChars, String closeQuoteChars, String breakIncludedChars, String breakExcludedChars, String unquotedIgnoredChars, String leadingIgnoredChars, String chars)
      {
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
       * @param chars
       */
      public JSONPathTokenizer withChars(String chars)
      {
         if (chars != null)
         {
            this.chars = chars.toCharArray();
         }
         head = 0;
         next = new StringBuffer();
         escaped = false;
         quoted = false;

         return this;
      }

      public List<String> asList()
      {
         List<String> list = new ArrayList();
         String next = null;
         while ((next = next()) != null)
            list.add(next);

         return list;
      }

      Set toSet(String string)
      {
         Set resultSet = new HashSet();
         for (int i = 0; i < string.length(); i++)
            resultSet.add(new Character(string.charAt(i)));

         return resultSet;
      }

      public String next()
      {
         if (head >= chars.length)
         {
            return null;
         }

         while (head < chars.length)
         {
            char c = chars[head];
            head += 1;

            //System.out.println("c = '" + c + "'");

            if (next.length() == 0 && leadingIgnored.contains(c))
               continue;

            if (c == escapeChar)
            {
               if (escaped)
                  append(c);

               escaped = !escaped;
               continue;
            }

            if (!quoted && unquotedIgnored.contains(c))
            {
               continue;
            }

            if (!quoted && !escaped && openQuotes.contains(c))
            {
               quoted = true;
            }
            else if (quoted && !escaped && closeQuotes.contains(c))
            {
               quoted = false;
            }

            if (!quoted && breakExcluded.contains(c) && next.length() > 0)
            {
               head--;
               break;
            }

            if (!quoted && breakIncluded.contains(c))
            {
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
         next = new StringBuffer();

         if (str.length() == 0)
            str = null;

         return str;
      }

      protected void append(char c)
      {
         next.append(c);
      }

   }

}
