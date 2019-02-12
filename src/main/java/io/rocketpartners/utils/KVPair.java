package io.rocketpartners.utils;

public class KVPair<K, V>
{
   public K key   = null;
   public V value = null;

   public KVPair(K key, V value)
   {
      super();
      this.key = key;
      this.value = value;
   }

   public K getKey()
   {
      return key;
   }

   public void setKey(K key)
   {
      this.key = key;
   }

   public V getValue()
   {
      return value;
   }

   public void setValue(V value)
   {
      this.value = value;
   }

}
