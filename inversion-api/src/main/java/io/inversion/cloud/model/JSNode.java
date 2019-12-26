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
package io.inversion.cloud.model;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
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

import io.inversion.cloud.utils.SimpleTokenizer;
import io.inversion.cloud.utils.Utils;

public class JSNode implements Map<String, Object>
{
   LinkedHashMap<String, Property> properties = new LinkedHashMap();

   public JSNode()
   {

   }

   public JSNode(Object... nvPairs)
   {
      with(nvPairs);
   }

   public JSNode(Map map)
   {
      for (Object key : map.keySet())
      {
         put(key + "", map.get(key));
      }
   }

   public JSArray diff(JSNode diffAgainst)
   {
      JSArray diffs = diff(diffAgainst, "", new JSArray());

      //we do this to prevent unintended consequences of copying in the same object references
      if (diffs.size() > 0)
         diffs = parseJsonArray(diffs.toString());

      return diffs;
   }

   protected JSArray diff(JSNode diffAgainst, String path, JSArray patches)
   {
      for (String key : keySet())
      {
         String nextPath = Utils.implode(".", path, key);

         Object myVal = get(key);
         Object theirVal = diffAgainst.get(key);

         diff(nextPath, myVal, theirVal, patches);
      }

      for (String key : diffAgainst.keySet())
      {
         Object myVal = get(key);
         Object theirVal = diffAgainst.get(key);

         if (myVal == null && theirVal != null)
            patches.add(new JSNode("op", "remove", "path", Utils.implode(".", path, key), myVal));
      }

      return patches;
   }

   protected void diff(String path, Object myVal, Object theirVal, JSArray patches)
   {
      if (myVal == null && theirVal == null)
      {

      }
      else if (myVal != null && theirVal == null)
      {
         patches.add(new JSNode("op", "add", "path", path, "value", myVal));
      }
      else if (myVal == null && theirVal != null)
      {
         patches.add(new JSNode("op", "remove", "path", path, myVal));
      }
      else if (!myVal.getClass().equals(theirVal.getClass()))
      {
         patches.add(new JSNode("op", "replace", "path", path, "value", myVal));
      }
      else if (myVal instanceof JSNode)
      {
         ((JSNode) myVal).diff((JSNode) theirVal, path, patches);
      }
      else if (!myVal.toString().equals(theirVal.toString()))
      {
         patches.add(new JSNode("op", "replace", "path", path, "value", myVal));
      }
   }

   public void patch(JSArray diffs)
   {
      //we do this to prevent unintended consequences of copying in the same object references
      diffs = parseJsonArray(diffs.toString());

      for (JSNode diff : diffs.asNodeList())
      {
         String op = diff.getString("op");
         String path = diff.getString("path");
         String prop = null;
         int idx = path.lastIndexOf(".");
         if (idx < 0)
         {
            prop = path;
            path = null;
         }
         else
         {
            prop = path.substring(idx + 1, path.length());
            path = path.substring(0, idx);
         }

         JSNode parent = path == null || path.length() == 0 ? this : findNode(path);

         if (parent == null)
            throw new RuntimeException("Unable to find parent path for patch '" + path + "'");
         if ("remove".equals(op))
         {
            try
            {
               parent.remove(prop);
            }
            catch (Exception e)
            {
               e.printStackTrace();
               System.err.println("You are trying to apply a property patch to an array: " + diff);
               throw e;
            }
         }
         else
         {
            parent.put(prop, diff.get("value"));
         }
      }
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
      JSArray found = collect(jsonPath, 1);
      if (found.size() > 0)
         return found.get(0);

      return null;
   }

   public List<JSNode> collectNodes(String jsonPath)
   {
      return (List<JSNode>) collect(jsonPath);
   }

   /**
    * @see collect(jsonPath, quantity)
    */
   public JSArray collect(String jsonPath)
   {
      return collect(jsonPath, -1);
   }

   /**
    * Runs the JsonPath expression against this node and
    * its children and returns any matching values.
    *
    * For an json path reference see:
    * <ul>
    *   <li> https://goessner.net/articles/JsonPath/
    *   <li> https://github.com/json-path/JsonPath
    * </ul>
    * 
    * Below is the implementation status of various JsonPath features:
    * <ul>
    *  <li>SUPPORTED $.store.book[*].author
    *  <li>SUPPORTED $..author
    *  <li>SUPPORTED $.store..price
    *  <li>SUPPORTED $..book[2]
    *  <li>SUPPORTED $..book[?(@.price<10)]
    *  <li>SUPPORTED $..book[?(@.author = 'Herman Melville')]
    *  <li>SUPPORTED $..*
    *  <li>TODO      $..book[(@.length-1)]
    *  <li>TODO      $..book[-1:]
    *  <li>TODO      $..book[0,1]
    *  <li>TODO      $..book[:2]
    *  <li>TODO      $..book[?(@.isbn)]
    * </ul>
    * 
    * The following boolean comparison operators are supported: 
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
    * In addition to the above JsonPath syntax, a "relaxed" wildcard
    * syntax is also supported. '*' is used to represent a single level
    * of freedom and '**' is used to represent freedom to match at any
    * depth.  Additionally, JsonPath uses array[idx] or array[*] notation
    * and the simplified wildcard supports array.${idxNum}.property 
    * or array.*.property or array.**.property
    * 
    */
   public JSArray collect(String jsonPath, int qty)
   {
      jsonPath = fromJsonPath(jsonPath);
      return new JSArray(collect0(jsonPath, qty));
   }

   public static String fromJsonPath(String jsonPath)
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
      SimpleTokenizer tok = new SimpleTokenizer(//
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
            SimpleTokenizer tokenizer = new SimpleTokenizer(//
                                                            "'\"", //openQuoteStr
                                                            "'\"", //closeQuoteStr
                                                            "]?", //breakIncludedChars
                                                            " ", //breakExcludedChars
                                                            "()", //unquuotedIgnoredChars
                                                            ". \t", //leadingIgoredChars
                                                            expr //chars
            );

            String token = null;
            String func = null;
            String subpath = null;
            String op = null;
            String value = null;

            while ((token = tokenizer.next()) != null)
            {
               if (token.equals("?"))
               {
                  func = "?";
                  continue;
               }

               if ("?".equals(func))
               {
                  if (token.startsWith("@_"))
                  {
                     subpath = token.substring(2);
                  }
                  else if (op == null && Utils.in(token, "=", ">", "<", ">=", "<=", "!="))
                  {
                     op = token;
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

      Property p = getProperty(name.toString());
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
      LinkedHashMap<String, Property> temp = new LinkedHashMap();

      Property prop = temp.put(name.toLowerCase(), new Property(name, value));

      this.properties.remove(name.toLowerCase());
      temp.putAll(this.properties);
      this.properties = temp;

      return prop;
   }

   @Override
   public Object put(String name, Object value)
   {
      Property prop = properties.put(name.toLowerCase(), new Property(name, value));
      return prop;
   }

   @Override
   public void putAll(Map map)
   {
      for (Object key : map.keySet())
      {
         put(key.toString(), map.get(key.toString()));
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

   public Property getProperty(String name)
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

      Property old = removeProperty(name.toString());
      return old != null ? old.getValue() : old;
   }

   @Override
   public Set<String> keySet()
   {
      //properties.getKeySet contains the lower case versions.
      LinkedHashSet keys = new LinkedHashSet();
      for (String key : properties.keySet())
      {
         Property p = properties.get(key);
         keys.add(p.getName());
      }
      return keys;
   }

   public boolean hasProperty(String name)
   {
      Property property = properties.get(name.toLowerCase());
      return property != null;
   }

   public List<Property> getProperties()
   {
      return new ArrayList(properties.values());
   }

   public Property removeProperty(String name)
   {
      Property property = properties.get(name.toLowerCase());
      if (property != null)
         properties.remove(name.toLowerCase());

      return property;
   }

   public static class Property
   {
      String name  = null;
      Object value = null;

      public Property(String name, Object value)
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
      Map map = new HashMap();
      for (Property p : properties.values())
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

      for (Property prop : properties.values())
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

         throw new RuntimeException("400 Bad Request: '" + json + "'");
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
      Property href = node.getProperty("href");

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
         Property p = node.getProperty(key);
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
}
