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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Yet another JavaScript/JSON list object representation with a few superpowers.
 * <p>
 * JSArray extends JSNode and JSNode implements Map so it is impossible for JSArray to implement List but that is practically what is happening.
 * <p>
 * This builds on JSNode but instead of string based keys, the keys are integer index positions in an underlying List.
 * 
 * @see JSNode
 */
public class JSArray extends JSNode implements Iterable {

   /**
    * The objects stored in this JSArray.  
    * <p>
    * This replaces the use of the JSNode.properties map from the superclass.
    */
   protected List objects = new ArrayList();

   public JSArray(Object... objects) {
      if (objects != null && objects.length == 1 && objects[0].getClass().isArray()) {
         objects = (Object[]) objects[0];
      } else if (objects != null && objects.length == 1 && java.util.Collection.class.isAssignableFrom(objects[0].getClass())) {
         objects = ((java.util.Collection) objects[0]).toArray();
      }

      for (int i = 0; objects != null && i < objects.length; i++)
         add(objects[i]);
   }

   /**
    * @return true
    * @see JSNode.isArray()
    */
   @Override
   public boolean isArray() {
      return true;
   }

   /**
    * Override of JSNode.put(String, Object) to parse <code>index</code> as an Integer and store the result in {@link #objects} instead of JSNode.properties
    * <p>
    * Put/set/add are basically synonyms.
    * 
    * @return the prior value at <code>index</code>
    * @see #set(int, Object)
    */
   @Override
   public Object put(String index, Object value) {
      return set(Integer.parseInt(index.trim()), value);
   }

   /**
    * Overloading of {@link #put(String, Object)} for completion, really just calls {@link #set(int, Object)}
    * <p>
    * Put/set/add are basically synonyms.
    * 
    * @param index
    * @param value
    * @return the prior value at <code>index</code>
    * @see #set(int, Object)
    */
   public Object put(int index, Object value) {
      return set(index, value);
   }

   /**
    * Sets <code>objects[index]</code> to value expanding the size of <code>objects</code> as much as is required. 
    * <p>
    * Put/set/add are basically synonyms.
    * 
    * @param index
    * @param value
    * @return the prior value at <code>index</code> if it exists
    * @throws ArrayIndexOutOfBoundsException only if index is < 0
    */
   public Object set(int index, Object value) throws ArrayIndexOutOfBoundsException {
      if (index < 0)
         throw new ArrayIndexOutOfBoundsException("You can't set index '" + index + "' on a list");

      while (objects.size() < index + 1)
         objects.add(null);

      return objects.set(index, value);
   }

   /**
    * Overloading of {@link #set(int, Object)}
    * 
    * @param index
    * @param object
    */
   public void add(int index, Object object) {
      objects.add(index, object);
   }

   /**
    * Adds <code>object</code> to the end of {@link #objects}
    * @param object
    */
   public void add(Object object) {
      objects.add(object);
   }

   /**
    * Adds all elements from <code>array</code> to the end of {@link #objects}
    * @param array
    */
   public void addAll(JSArray array) {
      objects.addAll(array.asList());
   }

   /**
    * Override of JSNode.remove(Object) to parse <code>index</code> as an Integer and remove the column from {@link #objects} instead of removing the key from JSNode.properties
    * @return the prior value at <code>index</code> if it exits
    * @see #remove(int)
    */
   @Override
   public Object remove(Object index) {
      return remove(Integer.parseInt(index.toString().trim()));
   }

   /**
    * Removes column <code>index</code> if it exists.
    * <p>
    * Will not throw ArrayIndexOutOfBoundsException
    * 
    * @param index the element/column to remove from {@link #objects}
    * @return the prior value at <code>index</code> if it exits
    */
   public Object remove(int index) {
      if (index < 0 || index >= objects.size())
         return null;

      return objects.remove(index);
   }

   /**
    * Override of JSNode.get(Object) to parse <code>index</code> as an Integer and pull from {@link #objects} instead of JSNode.properties
    */
   @Override
   public Object get(Object index) {
      return get(Integer.parseInt(index.toString().trim()));
   }

   /**
    * Gets the object at <code>index</code> and will not throw ArrayIndexOutOfBoundsException
    * @param index
    * @return the object at <code>index</code> if it exists else, null
    */
   public Object get(int index) {
      if (index > -1 && index >= objects.size())
         return null;
      return objects.get(index);
   }

   /** 
    * Convenience overloading of {@link #get(int)}
    * 
    * @param name
    * @return the value at <code>index</code> cast to a JSNode if exists else null
    * @throws ClassCastException if the object found is not a JSNode
    * @see #get(Object)
    */
   public JSNode getNode(int index) throws ClassCastException {
      return (JSNode) get(index);
   }

   /** 
    * Convenience overloading of {@link #get(Object)}
    * 
    * @param name
    * @return the value at <cod>index</code> cast to a JSArray if exists else null
    * @throws ClassCastException if the object found is not a JSArray
    * @see #get(Object)
    */
   public JSArray getArray(int index) {
      return (JSArray) get(index);
   }

   /**
    * Convenience overloading of {@link #get(Object)}
    * 
    * @param name
    * @return the value of property <code>name</code> stringified if it exists else null
    * @see #get(Object)
    */
   public String getString(int index) {
      Object value = get(index);
      if (value != null)
         return value.toString();
      return null;
   }

   /**
    * Convenience overloading of {@link #get(Object)}
    * 
    * @param name
    * @return the value at <cod>index</code> stringified and parsed as an int if it exists else -1
    * @see #get(Object)
    */
   public int getInt(int index) {
      Object found = get(index);
      if (found != null)
         return Utils.atoi(found);

      return -1;
   }

   /**
    * Convenience overloading of {@link #get(Object)}
    * 
    * @param name
    * @return the value at <cod>index</code> stringified and parsed as a double if it exists else -1
    * @see #get(Object)
    */
   public double getDouble(int index) {
      Object found = get(index);
      if (found != null)
         return Utils.atod(found);

      return -1;
   }

   /**
    * Convenience overloading of {@link #get(int)}
    * 
    * @param name
    * @return the value at <cod>index</code> stringified and parsed as a boolean if it exists else false
    * @see #get(Object)
    */
   public boolean getBoolean(int index) {
      Object found = get(index);
      if (found != null)
         return Utils.atob(found);

      return false;
   }

   @Override
   public Set<String> keySet() {
      //TODO make more efficient!!!!
      LinkedHashSet set = new LinkedHashSet();
      for (int i = 0; i < objects.size(); i++) {
         set.add(i + "");
      }
      return set;
   }

   public List asList() {
      return new ArrayList(this.objects);
   }

   /**
    * @return simply returns 'this';
    */
   public JSArray asArray() {
      return this;
   }

   public boolean contains(Object object) {
      return objects.contains(object);
   }

   @Override
   public boolean isEmpty() {
      return objects.isEmpty();
   }

   @Override
   public void clear() {
      objects.clear();
   }

   @Override
   public int size() {
      return objects.size();
   }

   public int length() {
      return objects.size();
   }

   @Override
   public Iterator iterator() {
      return asList().iterator();
   }

   @Override
   public Collection values() {
      return asList();
   }
}
