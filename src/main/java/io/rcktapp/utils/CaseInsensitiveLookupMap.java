/**
 * 
 */
package io.rcktapp.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * This map does not modify the case of the keys, but does allow for a
 * case-insensitive lookup
 *
 */
public class CaseInsensitiveLookupMap<K, V> extends HashMap<K, V>
{
   private static final long serialVersionUID = 7255504751110552067L;

   public CaseInsensitiveLookupMap()
   {
      super();
   }

   public CaseInsensitiveLookupMap(int initialCapacity, float loadFactor)
   {
      super(initialCapacity, loadFactor);
   }

   public CaseInsensitiveLookupMap(int initialCapacity)
   {
      super(initialCapacity);
   }

   public CaseInsensitiveLookupMap(Map<? extends K, ? extends V> m)
   {
      super(m);
   }

   @Override
   public V get(Object key)
   {
      V val = super.get(key);
      if (val == null)
      {
         String k = findCaseInsensitiveKey(key);
         if (k != null)
         {
            return super.get(k);
         }
      }
      return val;
   }

   @Override
   public V getOrDefault(Object key, V defaultValue)
   {
      V v = this.get(key);
      return v == null ? defaultValue : v;
   }

   @Override
   public boolean containsKey(Object key)
   {
      return this.get(key) != null;
   }

   @Override
   public V remove(Object key)
   {
      V val = super.remove(key);
      if (val == null)
      {
         String k = findCaseInsensitiveKey(key);
         if (k != null)
         {
            return super.remove(k);
         }
      }
      return val;

   }

   private String findCaseInsensitiveKey(Object key)
   {
      if (key instanceof String)
      {
         for (K k : super.keySet())
         {
            if (((String) key).equalsIgnoreCase(((String) k)))
            {
               return (String) k;
            }
         }
      }
      return null;
   }

   public static void main(String[] args)
   {
      CaseInsensitiveLookupMap m = new CaseInsensitiveLookupMap();
      m.put("dog", "foo");
      m.put("Cat", "foo");
      m.put("FISH", "foo");
      m.put("biRD", "foo");

      testNotNull(m, "dog");
      testNotNull(m, "DOG");
      testNotNull(m, "Dog");
      testNotNull(m, "cat");
      testNotNull(m, "CAT");
      testNotNull(m, "Cat");
      testNotNull(m, "fish");
      testNotNull(m, "FISH");
      testNotNull(m, "Fish");
      testNotNull(m, "bird");
      testNotNull(m, "BIRD");
      testNotNull(m, "Bird");
      testNotNull(m, "biRD");

      test(m.getOrDefault("Dog", "default").equals("foo"), "Should have a value for this key");
      test(m.getOrDefault("Blah", "default").equals("default"), "Should use default value");

      test(m.containsKey("Dog"), "Should contain Dog");
      test(!m.containsKey("Blah"), "Should not contain Blah");

      test(m.keySet().size() == 4, "Initial map is 4");
      test((m.remove("Dog") != null), "Should remove something");
      test((m.remove("Blah") == null), "Should not remove something");
      test(m.keySet().size() == 3, "Map after remove is 3");

   }

   static void testNotNull(CaseInsensitiveLookupMap m, String field)
   {
      boolean pass = m.get(field) != null;
      test(pass, field);
   }

   static void test(boolean pass, String msg)
   {
      System.out.println(pass ? "     PASS " + msg : " *** FAIL " + msg);
   }

}
