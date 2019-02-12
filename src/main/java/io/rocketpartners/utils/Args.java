/*
 * Copyright (c) 2016-2019 Rocket Partners, LLC
 * http://rocketpartners.io
 * 
 * Copyright 2008-2016 Wells Burke
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
package io.rocketpartners.utils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class Args
{
   /**
    * Parses the specified command line into an array of individual arguments.
    * Arguments containing spaces should be enclosed in quotes.
    * Quotes that should be in the argument string should be escaped with a
    * preceding backslash ('\') character.  Backslash characters that should
    * be in the argument string should also be escaped with a preceding
    * backslash character.
    * @param args the command line to parse
    * @return an argument array representing the specified command line.
    */
   public static String[] parse(String args)
   {
      List resultBuffer = new java.util.ArrayList();

      if (args != null)
      {
         args = args.trim();
         int z = args.length();
         boolean insideQuotes = false;
         StringBuffer buf = new StringBuffer();

         for (int i = 0; i < z; ++i)
         {
            char c = args.charAt(i);
            if (c == '"')
            {
               appendToBuffer(resultBuffer, buf);
               insideQuotes = !insideQuotes;
            }
            else if (c == '\\')
            {
               if ((z > i + 1) && ((args.charAt(i + 1) == '"') || (args.charAt(i + 1) == '\\')))
               {
                  buf.append(args.charAt(i + 1));
                  ++i;
               }
               else
               {
                  buf.append("\\");
               }
            }
            else
            {
               if (insideQuotes)
               {
                  buf.append(c);
               }
               else
               {
                  if (Character.isWhitespace(c))
                  {
                     appendToBuffer(resultBuffer, buf);
                  }
                  else
                  {
                     buf.append(c);
                  }
               }
            }
         }
         appendToBuffer(resultBuffer, buf);

      }

      String[] result = new String[resultBuffer.size()];
      return ((String[]) resultBuffer.toArray(result));
   }

   private static void appendToBuffer(List resultBuffer, StringBuffer buf)
   {
      if (buf.length() > 0)
      {
         resultBuffer.add(buf.toString());
         buf.setLength(0);
      }
   }

   public static <T> T config(T obj, String[] a)
   {
      Args args = new Args(a);
      args.setAll(obj);
      return obj;
   }

   Map<String, String> args = new HashMap();

   public Args()
   {

   }

   public Args(String[] args)
   {
      for (int i = 0; args != null && i < args.length - 1; i++)
      {
         if (args[i].startsWith("-"))
         {
            this.args.put(args[i].substring(1, args[i].length()).trim().toLowerCase(), args[i + 1]);
            i += 1;
         }
      }
   }

   public Args put(Properties p)
   {
      for (Object key : p.keySet())
      {
         put((String) key, p.getProperty((String) key));
      }
      return this;
   }

   public Args put(String key, String value)
   {
      args.put(key.toLowerCase(), value);
      return this;
   }

   public Args putDefault(String key, String value)
   {
      if (getArg(key) == null)
         args.put(key.toLowerCase(), value);

      return this;
   }

   public Args remove(String key)
   {
      args.remove(key.toLowerCase());
      return this;
   }

   public String getArg(String name)
   {
      return getArg(name, null);
   }

   public String getArg(String name, String deafultValue)
   {
      String value = args.get(name.toLowerCase());

      if (value == null)
      {
         value = System.getProperty(name);
         if (value == null)
         {
            for (Object key : System.getProperties().keySet())
            {
               if (name.equalsIgnoreCase((String) key))
               {
                  value = System.getProperty((String) key);
                  break;
               }
            }
         }
      }

      if (value == null)
      {
         value = System.getenv(name);
      }

      return value != null ? value : deafultValue;
   }

   public Args setAll(Object target)
   {
      try
      {
         Set done = new HashSet();
         Class clazz = target.getClass();
         do
         {
            Field[] fields = clazz.getDeclaredFields();
            for (int i = 0; fields != null && i < fields.length; i++)
            {
               Field f = fields[i];
               String name = f.getName().toLowerCase();
               //System.out.println(name);

               if (done.contains(name))
                  continue;

               done.add(name);

               String value = getArg(name);
               if (value != null)
               {
                  f.setAccessible(true);

                  Class type = f.getType();
                  if (type.isAssignableFrom(String.class))
                  {
                     f.set(target, value);
                  }
                  else if (type.isAssignableFrom(int.class))
                  {
                     f.set(target, Integer.parseInt(value));
                  }
                  else if (type.isAssignableFrom(long.class))
                  {
                     f.set(target, Long.parseLong(value));
                  }
                  else if (type.isAssignableFrom(boolean.class))
                  {
                     boolean val = value.toLowerCase().startsWith("t") || value.equals("1");
                     f.set(target, val);
                  }
                  else if (Collection.class.isAssignableFrom(type))
                  {
                     Collection c = (Collection) f.get(target);
                     c.clear();
                     c.addAll(Arrays.asList(value.split(",")));
                  }
                  else
                  {
                     System.out.println("unknown type:" + type);
                  }
               }
            }
            clazz = clazz.getSuperclass();
         }
         while (clazz != null && !clazz.getPackage().getName().startsWith("java"));
      }
      catch (Exception ex)
      {
         throw new RuntimeException(ex);
      }
      return this;
   }
}
