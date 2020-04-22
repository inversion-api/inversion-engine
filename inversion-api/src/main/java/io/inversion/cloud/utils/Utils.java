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
package io.inversion.cloud.utils;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import com.fasterxml.jackson.databind.util.ISO8601Utils;

import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.JSArray;
import io.inversion.cloud.model.JSNode;

/**
 * Collection of utility methods designed to make
 * java programming less verbose
 *
 * @author Wells Burke
 */
public class Utils
{
   public static final int         KB                 = 1048;
   public static final int         MB                 = 1048576;
   public static final long        GB                 = 1073741824;
   public static final int         K64                = 65536;

   public static final long        HOUR               = 1000 * 60 * 60;
   public static final long        DAY                = 1000 * 60 * 60 * 24;
   public static final long        MONTH              = 1000 * 60 * 60 * 24 * 31;
   public static final long        WEEK               = 1000 * 60 * 60 * 24 * 7;
   public static final long        YEAR               = 1000 * 60 * 60 * 24 * 365;

   protected static final String   NEW_LINE           = System.getProperty("line.separator");

   protected static final String[] EMPTY_STRING_ARRAY = new String[0];

   /**
    * A null safe loose equality checker.
    * @param obj1
    * @param obj2
    * @return Test for strict == equality, then .equals() equality, then .toString().equals() equality.  Either param can be null.
    */
   public static boolean equal(Object obj1, Object obj2)
   {
      if (obj1 == obj2)
         return true;

      if (obj1 == null || obj2 == null)
         return false;

      return obj1.toString().equals(obj2.toString());
   }

   public static boolean in(Object toFind, Object... values)
   {
      for (Object val : values)
      {
         if (equal(toFind, val))
            return true;
      }
      return false;
   }

   /**
    * @return true if any args are not null with a toString().length() > 0
    */
   public static boolean empty(Object... arr)
   {
      boolean empty = true;
      for (int i = 0; empty && arr != null && i < arr.length; i++)
      {
         Object obj = arr[i];
         if (obj != null && obj.toString().length() > 0)
            empty = false;
      }
      return empty;
   }

   public static Object first(List list)
   {
      if (list.size() > 0)
         return list.get(0);
      return null;
   }

   public static Object last(List list)
   {
      if (list.size() > 0)
         return list.get(list.size() - 1);
      return null;
   }

   /**
    * @param glue
    * @param pieces
    * @return Concatenates pieces[0] + glue + pieces[n]... Intelligently recurses through Collections
    */
   public static String implode(String glue, Object... pieces)
   {
      if (pieces != null && pieces.length == 1 && pieces[0] instanceof Collection)
         pieces = ((Collection) pieces[0]).toArray();

      StringBuffer str = new StringBuffer("");
      for (int i = 0; pieces != null && i < pieces.length; i++)
      {
         if (pieces[i] != null)
         {
            String piece = pieces[i] instanceof Collection ? implode(glue, pieces[i]) : pieces[i].toString();

            if (piece.length() > 0)
            {
               List<String> subpieces = explode(glue, piece);

               for (String subpiece : subpieces)
               {
                  if (subpiece.length() > 0)
                  {
                     if (str.length() > 0)
                        str.append(glue);
                     str.append(subpiece);
                  }
               }
            }
         }
      }
      return str.toString();
   }

   /**
    * @param delim
    * @param pieces
    * @return Same as String.split() but performs a trim() on each piece and returns an list instead of an array
    */
   public static List<String> explode(String delim, String... pieces)
   {
      if (".".equals(delim))
         delim = "\\.";

      List exploded = new ArrayList();
      for (int i = 0; pieces != null && i < pieces.length; i++)
      {
         if (Utils.empty(pieces[i]))
            continue;

         String[] parts = pieces[i].split(delim);
         for (int j = 0; j < parts.length; j++)
         {
            String part = parts[j].trim();
            if (!Utils.empty(part))
            {
               exploded.add(part);
            }
         }
      }

      return exploded;
   }

   /**
    * Breaks the string on <code>splitOn</code> but when inside a <code>quoteChars</code>
    * quoted string.
    * 
    * @param string
    * @param splitOn
    * @param quoteChars
    * @return
    */
   public static List<String> split(String string, char splitOn, char... quoteChars)
   {
      List<String> strings = new ArrayList();
      Set quotes = new HashSet();
      for (char c : quoteChars)
         quotes.add(c);

      boolean quoted = false;
      StringBuffer buff = new StringBuffer("");
      for (int i = 0; i < string.length(); i++)
      {
         char c = string.charAt(i);

         if (c == splitOn && !quoted)
         {
            if (buff.length() > 0)
            {
               strings.add(buff.toString());
               buff = new StringBuffer("");
            }
            continue;
         }
         else if (quotes.contains(c))
         {
            quoted = !quoted;
         }

         buff.append(c);
      }
      if (buff.length() > 0)
         strings.add(buff.toString());

      return strings;
   }

   public static String substringBefore(String string, String breakBefore)
   {
      int idx = string.indexOf(breakBefore);
      if (idx > -1)
         string = string.substring(0, idx);

      return string;
   }

   public static String substringAfter(String string, String breakAfterLast)
   {
      int idx = string.lastIndexOf(breakAfterLast);
      if (idx > -1)
         string = string.substring(idx + breakAfterLast.length());

      return string;
   }

   public static ArrayListValuedHashMap addToMap(ArrayListValuedHashMap map, String... keyValuePairs)
   {
      if (keyValuePairs != null && keyValuePairs.length % 2 > 0)
         throw new RuntimeException("keyValuePairs.length must be evenly divisible by 2.");

      for (int i = 0; keyValuePairs != null && i < keyValuePairs.length - 1; i += 2)
         map.put(keyValuePairs[i], keyValuePairs[i + 1]);

      return map;
   }

   public static <M extends Map> M addToMap(M map, String... keyValuePairs)
   {
      if (keyValuePairs != null && keyValuePairs.length % 2 > 0)
         throw new RuntimeException("keyValuePairs.length must be evenly divisible by 2.");

      for (int i = 0; keyValuePairs != null && i < keyValuePairs.length - 1; i += 2)
         map.put(keyValuePairs[i], keyValuePairs[i + 1]);

      return map;
   }

   public static List asList(Object... objects)
   {
      return Arrays.asList(objects);
   }

   public static Set asSet(Object... objects)
   {
      return new HashSet(Arrays.asList(objects));
   }

   public static Map asMap(Object... objects)
   {
      Map map = new HashMap();
      for (int i = 0; objects != null && i < objects.length - 1; i += 2)
      {
         map.put(objects[i], objects[i + 1]);
      }
      return map;
   }

   /**
    * Checks for a whole word case insensitive match of <code>findThisToken</code>
    * in <code>inThisString</code>
    * 
    * https://www.baeldung.com/java-regexp-escape-char
    * https://stackoverflow.com/questions/7459263/regex-whole-word
    * 
    * @param findThisToken
    * @param inThisString
    */
   public static boolean containsToken(String findThisToken, String inThisString)
   {
      String regex = "\\b\\Q" + findThisToken + "\\E\\b";
      return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(inThisString).find();
   }

   /**
    * Removes all matching pairs of '"` characters from the
    * start and end of a string.
    * @param str
    * @return
    */
   public static String dequote(String str)
   {
      return dequote(str, new char[]{'\'', '"', '`'});
   }

   public static String dequote(String str, char[] quoteChars)
   {
      if (str == null)
         return null;

      while (str.length() >= 2 && str.charAt(0) == str.charAt(str.length() - 1))// && (str.charAt(0) == '\'' || str.charAt(0) == '"' || str.charAt(0) == '`'))
      {
         boolean changed = false;
         for (int i = 0; i < quoteChars.length; i++)
         {
            if (str.charAt(0) == quoteChars[i])
            {
               str = str.substring(1, str.length() - 1);
               changed = true;
               break;
            }
         }
         if (!changed)
            break;
      }

      return str;
   }

   /**
    * Turns a double value into a rounded double with 2 digits of precision
    * 12.3334 -> 12.33
    * 23.0 -> 23.00
    * 45.677 -> 45.68
    * @param amount
    * @return
    */
   public static BigDecimal toDollarAmount(double amount)
   {
      return new BigDecimal(amount).setScale(2, BigDecimal.ROUND_HALF_UP);
   }

   public static int roundUp(int num, int divisor)
   {
      int sign = (num > 0 ? 1 : -1) * (divisor > 0 ? 1 : -1);
      return sign * (Math.abs(num) + Math.abs(divisor) - 1) / Math.abs(divisor);
   }

   /**
    * Faster and null safe way to call Integer.parseInt(str.trim()) that swallows exceptions.
    */
   public static boolean atob(Object str)
   {
      try
      {
         String bool = str + "";
         return !("0".equals(bool) || "false".equals(bool));
      }
      catch (Exception ex)
      {
         //ignore
      }
      return false;
   }

   /**
    * Faster and null safe way to call Integer.parseInt(str.trim()) that swallows exceptions.
    */
   public static int atoi(Object str)
   {
      try
      {
         return Integer.parseInt(str.toString().trim());
      }
      catch (Exception ex)
      {
         //ignore
      }
      return -1;
   }

   /**
    * Faster and null safe way to call Long.parseLong(str.trim()) that swallows exceptions.
    */
   public static long atol(Object str)
   {
      try
      {
         return Long.parseLong(str.toString().trim());
      }
      catch (Exception ex)
      {
         //ignore
      }
      return -1;
   }

   /**
    * Faster and null safe way to call Float.parseFloat(str.trim()) that swallows exceptions.
    */
   public static float atof(Object str)
   {
      try
      {
         return Float.parseFloat(str.toString().trim());
      }
      catch (Exception ex)
      {
         //ignore
      }
      return -1;
   }

   /**
    * Faster and null safe way to call Double.parseDouble(str.trim()) that swallows exceptions.
    */
   public static double atod(Object str)
   {
      try
      {
         return Double.parseDouble(str.toString().trim());
      }
      catch (Exception ex)
      {
         //ignore
      }
      return -1;
   }

   /**
    * returns a lowercased url safe string
    * @param str
    * @return
    */
   public static String slugify(String str)
   {
      if (str == null)
      {
         return null;
      }

      str = str.toLowerCase().trim();

      str = str.replaceAll("[']+", "");
      str = str.replaceAll("[^a-z0-9]+", "-");

      //removes consecutive -'s
      str = str.replaceAll("([\\-])(\\1{2,})", "$1");

      // remove preceding and trailing dashes
      str = str.replaceAll("^-", "");
      str = str.replaceAll("-$", "");

      return str;
   }

   /**
    * @param bytes
    * @return Hash the bytes with SHA-1
    */
   public static String sha1(byte[] bytes)
   {
      return hash(bytes, "SHA-1");
   }

   /**
    * @param bytes
    * @return Hash the bytes with MD5
    */
   public static String md5(byte[] bytes)
   {
      return hash(bytes, "MD5");
   }

   /**
    * @param byteArr
    * @param algorithm
    * @return Hash the bytes with the given algorithm
    */
   public static String hash(byte[] byteArr, String algorithm)
   {
      try
      {
         MessageDigest digest = MessageDigest.getInstance(algorithm);
         digest.update(byteArr);
         byte[] bytes = digest.digest();

         String hex = (new HexBinaryAdapter()).marshal(bytes);

         return hex;
      }
      catch (Exception ex)
      {
         rethrow(ex);
      }
      return null;
   }

   /**
    * Less typing to call System.currentTimeMillis()
    */
   public static long time()
   {
      return System.currentTimeMillis();
   }

   //   public static String formatDate(Date date)
   //   {
   //      TimeZone tz = TimeZone.getTimeZone("UTC");
   //      DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
   //      df.setTimeZone(tz);
   //      return df.format(date);
   //   }

   public static Date parseIso8601(String date) throws ParseException
   {
      return ISO8601Utils.parse(date, new ParsePosition(0));
   }

   public static String formatIso8601(Date date)
   {
      return ISO8601Utils.format(date);
   }

   /**
    * Simple one liner to avoid verbosity of using SimpleDateFormat
    */
   public static String formatDate(Date date, String format)
   {
      SimpleDateFormat f = new SimpleDateFormat(format);
      return f.format(date);
   }

   /**
    * Faster way to apply a SimpleDateForamt without having to catch ParseException
    * @param date
    * @param format
    */
   public static Date date(String date, String format)
   {
      try
      {
         date = date.trim();
         SimpleDateFormat df = new SimpleDateFormat(format);
         return df.parse(date);
      }
      catch (Exception ex)
      {
         rethrow(ex);
      }
      return null;
   }

   /**
    * Attempts an ISO8601 data as yyyy-MM-dd|yyyyMMdd][T(hh:mm[:ss[.sss]]|hhmm[ss[.sss]])]?[Z|[+-]hh[:]mm],
    * then yyyy-MM-dd,
    * then MM/dd/yy,
    * then MM/dd/yyyy,
    * then yyyyMMdd
    * @param date
    * @return
    */
   public static Date date(String date)
   {
      try
      {
         //not supported in JDK 1.6
         //         DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
         //         TemporalAccessor accessor = timeFormatter.parse(date);
         //         return Date.from(Instant.from(accessor));
         return ISO8601Utils.parse(date, new ParsePosition(0));
      }
      catch (Exception ex)
      {

      }
      try
      {
         SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
         return f.parse(date);

      }
      catch (Exception ex)
      {

      }

      try
      {
         SimpleDateFormat f = new SimpleDateFormat("MM/dd/yy");

         int lastSlash = date.lastIndexOf("/");
         if (lastSlash > 0 && lastSlash == date.length() - 5)
         {
            f = new SimpleDateFormat("MM/dd/yyyy");
         }
         Date d = f.parse(date);
         //System.out.println(d);
         return d;

      }
      catch (Exception ex)
      {

      }

      try
      {
         SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
         return f.parse(date);
      }
      catch (Exception ex)
      {
         throw new RuntimeException("unsupported format: " + date);
      }

   }

   public static boolean testCompare(String str1, String str2)
   {
      str1 = str1 != null ? str1 : "";
      str2 = str2 != null ? str2 : "";

      str1 = str1.replaceAll("\\s+", " ").trim();
      str2 = str2.replaceAll("\\s+", " ").trim();

      if (!str1.equals(str2))
      {
         if (!str1.equals(str2))
         {
            System.out.println("\r\n");
            System.out.println("\r\n");
            System.out.println(str1);
            System.out.println(str2);

            for (int i = 0; i < str1.length() && i < str2.length(); i++)
            {
               if (str1.charAt(i) == str2.charAt(i))
               {
                  System.out.print(" ");
               }
               else
               {
                  System.out.println("X");
                  break;
               }
            }
            System.out.println(" ");
            return false;
         }
      }
      return true;
   }

   /**
    * Tries to \"unwrap\" nested exceptions looking for the root cause
    * @param t
    */
   public static Throwable getCause(Throwable t)
   {
      Throwable origional = t;

      int guard = 0;
      while (t != null && t.getCause() != null && t.getCause() != t && guard < 100)
      {
         t = t.getCause();
         guard++;
      }

      if (t == null)
      {
         t = origional;
      }

      return t;
   }

   /**
    * Shortcut for throw new RuntimeException(message);
    */
   public static void error(String message)
   {
      throw new RuntimeException(message);
   }

   /**
    * Throws the root cause of e as a RuntimeException
    * @param e
    */
   public static void rethrow(Throwable e)
   {
      rethrow(null, e);
   }

   /**
    * Throws the root cause of e as a RuntimeException
    * @param e
    */
   public static void rethrow(String message, Throwable e)
   {
      Throwable cause = e;

      while (cause.getCause() != null && cause.getCause() != e)
         cause = cause.getCause();

      if (cause instanceof RuntimeException)
      {
         throw (RuntimeException) cause;
      }

      if (e instanceof RuntimeException)
         throw (RuntimeException) e;

      if (!empty(message))
      {
         throw new RuntimeException(message, e);
      }
      else
      {
         throw new RuntimeException(e);
      }
   }

   /**
    * Easy way to call Thread.sleep(long) without worrying about try/catch for InterruptedException
    * @param millis
    */
   public static void sleep(long millis)
   {
      try
      {
         Thread.sleep(millis);
      }
      catch (InterruptedException e)
      {
         rethrow(e);
      }
   }

   public static String getShortCause(Throwable t)
   {
      return getShortCause(t, 15);
   }

   public static String getShortCause(Throwable t, int lines)
   {
      t = getCause(t);
      //return System.getProperty("line.separator") + limitLines(clean(getStackTraceString(t)), lines);
      return limitLines(cleanStackTrace(getStackTraceString(t)), lines);
   }

   public static List<String> getStackTraceLines(Throwable stackTrace)
   {
      ByteArrayOutputStream baos = null;
      PrintWriter writer;

      baos = new ByteArrayOutputStream();
      writer = new PrintWriter(baos);

      if (stackTrace != null)
      {
         stackTrace.printStackTrace(writer);
      }
      else
      {
         try
         {
            throw new Exception();
         }
         catch (Exception e)
         {
            e.printStackTrace(writer);
         }
      }

      writer.close();

      List lines = new ArrayList();
      String s = new String(baos.toByteArray());
      String[] sArr = s.split("\n");
      lines.addAll(new ArrayList(Arrays.asList(sArr)));

      return lines;
   }

   public static String getStackTraceString(Throwable stackTrace)
   {
      ByteArrayOutputStream baos = null;
      PrintWriter writer;

      baos = new ByteArrayOutputStream();
      writer = new PrintWriter(baos);

      boolean createNewTrace = false;

      if (stackTrace != null)
      {
         try
         {
            stackTrace.printStackTrace(writer);
         }
         catch (Exception e)
         {
            createNewTrace = true;
         }
      }
      else
      {
         createNewTrace = true;
      }

      if (createNewTrace)
      {
         try
         {
            throw new Exception("Unable to get original stacktrace.");
         }
         catch (Exception e)
         {
            e.printStackTrace(writer);
         }
      }

      writer.close();

      return new String(baos.toByteArray());

   }

   static String cleanStackTrace(String stackTrace)
   {
      String[] ignoredCauses = new String[]{//
            //
            "java.lang.reflect.UndeclaredThrowableException", //
            "java.lang.reflect.InvocationTargetException"};

      String[] lines = splitLines(stackTrace);

      boolean chop = false;
      if (stackTrace.indexOf("Caused by: ") > 0)
      {
         for (int i = 0; i < ignoredCauses.length; i++)
         {
            if (lines[0].indexOf(ignoredCauses[i]) > -1)
            {
               chop = true;
               break;
            }
         }
      }

      int start = 0;
      if (chop)
      {
         for (int i = 0; i < lines.length; i++)
         {
            if (lines[i].startsWith("Caused by:"))
            {
               lines[i] = lines[i].substring(10, lines[i].length());
               break;
            }

            start++;
         }
      }

      StringBuffer buffer = new StringBuffer();
      for (int i = start; i < lines.length; i++)
      {
         buffer.append(lines[i]).append("\r\n");
      }

      if (chop)
      {
         return cleanStackTrace(buffer.toString());
      }
      else
      {
         return buffer.toString();
      }
   }

   public static String[] splitLines(String text)
   {
      if (text == null || "".equals(text))
      {
         return EMPTY_STRING_ARRAY;
      }

      String lineSeparator = text.indexOf(NEW_LINE) >= 0 ? NEW_LINE : "\n";
      return text.split(lineSeparator);
   }

   public static final String limitLines(String text, int limit)
   {
      StringBuffer buffer = new StringBuffer("");
      String[] lines = splitLines(text);
      for (int i = 0; i < lines.length && i < limit; i++)
      {
         if (i == limit - 1 && i != lines.length - 1)
         {
            buffer.append("..." + (lines.length - i) + " more");
         }
         else
         {
            buffer.append(lines[i]).append(NEW_LINE);
         }
      }

      return buffer.toString();
   }

   //   /**
   //    * Same as calling Class.getMethod but it returns null intead of throwing a NoSuchMethodException
   //    * @param clazz
   //    * @param name
   //    * @param args
   //    * @return
   //    */
   //   public static Method getMethod(Class clazz, String name, Class... args)
   //   {
   //      try
   //      {
   //         return clazz.getMethod(name, args);
   //      }
   //      catch (NoSuchMethodException ex)
   //      {
   //
   //      }
   //      return null;
   //   }
   //
   /**
    * Searches the inheritance hierarchy for a field with the the given name and makes sure it is settable via Field.setAccesible
    * @param fieldName
    * @param clazz
    * @return
    */
   public static Field getField(String fieldName, Class clazz)
   {
      if (fieldName == null || clazz == null)
      {
         return null;
      }

      Field[] fields = clazz.getDeclaredFields();
      for (int i = 0; i < fields.length; i++)
      {
         if (fields[i].getName().equals(fieldName))
         {
            Field field = fields[i];
            field.setAccessible(true);
            return field;
         }
      }

      if (clazz.getSuperclass() != null && !clazz.equals(clazz.getSuperclass()))
      {
         return getField(fieldName, clazz.getSuperclass());
      }

      return null;
   }

   /**
    * Gets all the fields from from all classes in the inheritance hierarchy EXCEPT for any class who's packages starts with \"java\"
    * @param clazz
    * @return
    */
   public static List<Field> getFields(Class clazz)
   {
      Class inClass = clazz;
      Set found = new HashSet();
      List<Field> fields = new ArrayList();

      do
      {
         if (clazz.getName().startsWith("java"))
            break;

         Field[] farr = clazz.getDeclaredFields();
         if (farr != null)
         {
            for (Field f : farr)
            {
               if (!found.contains(f.getName()))
               {
                  f.setAccessible(true);
                  found.add(f.getName());
                  fields.add(f);
               }
               else
               {
                  System.out.println("This super class property is being skipped because it is being hidden by a child class property with the same name...is this a design mistake? " + f);
               }
            }
         }
         clazz = clazz.getSuperclass();
      }
      while (clazz != null && !Object.class.equals(clazz));

      return fields;
   }

   //
   //   /**
   //    * Finds the Field in the inheritance heirarchy and sets it
   //    * @param name
   //    * @param value
   //    * @param o
   //    * @throws NoSuchFieldException
   //    * @throws IllegalAccessException
   //    */
   //   public static void setField(String name, Object value, Object o) throws NoSuchFieldException, IllegalAccessException
   //   {
   //      Field f = getField(name, o.getClass());
   //      f.setAccessible(true);
   //      f.set(o, value);
   //   }
   //
   /**
    * Searches the inheritance hierarchy for the first method of the given name (ignores case).  No distinction is made for overloaded method names.
    * @param clazz
    * @param name
    * @return
    */
   public static Method getMethod(Class clazz, String name)
   {
      do
      {
         for (Method m : clazz.getMethods())
         {
            if (m.getName().equalsIgnoreCase(name))
               return m;
         }

         if (clazz != null)
         {
            clazz = clazz.getSuperclass();
         }
      }
      while (clazz != null && !Object.class.equals(clazz));

      return null;
   }

   /**
    * Tries to find a bean property getter then defaults to returning the Field value
    * @param name
    * @param object
    * @return
    */
   public static Object getProperty(String name, Object object)
   {
      try
      {
         Method getter = getMethod(object.getClass(), "get" + name);
         if (getter != null)
         {
            return getter.invoke(object);
         }
         else
         {
            Field field = getField(name, object.getClass());
            if (field != null)
               return field.get(object);
         }
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
      }
      return null;
   }

   /**
    * Read all of the stream to a string and close the stream.  Throws RuntimeException instead of IOException
    * @param in
    * @return
    */
   public static String read(InputStream in)
   {
      try
      {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         pipe(in, out);
         return new String(out.toByteArray());
      }
      catch (Exception ex)
      {
         rethrow(ex);
      }
      return null;
   }

   /**
    * Read teh contents of a file to a string
    * @param file
    * @return
    * @throws Exception
    */
   public static String read(File file) throws Exception
   {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      FileInputStream in = new FileInputStream(file);
      pipe(in, out);
      return new String(out.toByteArray());
   }

   /**
    * Write the string value to a file
    * @param file
    * @param text
    * @throws Exception
    */
   public static void write(File file, String text) throws Exception
   {
      if (!file.exists())
         file.getParentFile().mkdirs();

      BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
      bw.write(text);
      bw.flush();
      bw.close();
   }

   /**
    * Write the string value to a file
    * @param file
    * @param text
    * @throws Exception
    */
   public static void write(String file, String text) throws Exception
   {
      if (text == null)
         return;
      write(new File(file), text);
   }

   /**
    * Copy all data from src to dst and close the streams
    * @param src
    * @param dest
    * @throws Exception
    */
   public static void pipe(InputStream src, OutputStream dest) throws Exception
   {
      try
      {
         boolean isBlocking = true;
         byte[] buf = new byte[K64];

         int nread;
         int navailable;
         //int total = 0;
         synchronized (src)
         {
            while ((navailable = isBlocking ? Integer.MAX_VALUE : src.available()) > 0 //
                  && (nread = src.read(buf, 0, Math.min(buf.length, navailable))) >= 0)
            {
               dest.write(buf, 0, nread);
               //total += nread;
            }
         }
         dest.flush();

      }
      finally
      {
         close(src);
         close(dest);
      }
   }

   public static File createTempFile(String fileName) throws IOException
   {
      if (empty(fileName))
         fileName = "working.tmp";

      fileName = fileName.trim();
      fileName = fileName.replace('\\', '/');

      if (fileName.endsWith("/"))
      {
         fileName = "working.tmp";
      }
      else
      {
         if (fileName.lastIndexOf('/') > 0)
         {
            fileName = fileName.substring(fileName.lastIndexOf('/') + 1, fileName.length());
         }
      }

      if (empty(fileName))
         fileName = "working.tmp";

      fileName = slugify(fileName);
      if (fileName.lastIndexOf('.') > 0)
      {
         String prefix = fileName.substring(0, fileName.lastIndexOf('.'));
         String suffix = fileName.substring(fileName.lastIndexOf('.'), fileName.length());

         return File.createTempFile(prefix + "-", suffix);
      }
      else
      {
         return File.createTempFile(fileName, "");
      }
   }

   /**
    * Attempts to locate the stream as a file, url, or classpath resource
    * @param fileOrUrl
    * @return
    */
   public static InputStream findInputStream(String fileOrUrl)
   {
      try
      {
         if (fileOrUrl.startsWith("file:/"))
         {
            fileOrUrl = URLDecoder.decode(fileOrUrl, "UTF-8");
         }
         if (fileOrUrl.startsWith("file:///"))
         {
            fileOrUrl = fileOrUrl.substring(7, fileOrUrl.length());
         }
         if (fileOrUrl.startsWith("file:/"))
         {
            fileOrUrl = fileOrUrl.substring(5, fileOrUrl.length());
         }

         if (fileOrUrl.indexOf(':') >= 0)
         {
            return new URL(fileOrUrl).openStream();
         }
         else if (new File(fileOrUrl).exists())
         {
            return new FileInputStream(fileOrUrl);
         }
         else
         {
            return Thread.currentThread().getContextClassLoader().getResourceAsStream(fileOrUrl);
         }
      }
      catch (Exception ex)
      {
         if (ex instanceof RuntimeException)
            throw (RuntimeException) ex;

         throw new RuntimeException(ex);
      }
   }

   /**
    * Returns true if the string contains a * or a ?
    */
   public static boolean isWildcard(String str)
   {
      return str.indexOf('*') >= 0 || str.indexOf('?') >= 0;
   }

   /**
    * Pattern matches the string using ? to indicate any one single value and * to indicate any 0-n multiple values
    */
   public static boolean wildcardMatch(String wildcard, String string)
   {
      if (wildcard.equals("*"))
         return true;

      if (empty(wildcard) || empty(string))
         return false;

      if (!isWildcard(wildcard))
         return wildcard.equals(string);
      else
         return regexMatch(wildcardToRegex(wildcard), string);
   }

   /**
    * Performs string.matches() but also checks for null
    *
    * @param regex
    * @param string
    * @return
    */
   public static boolean regexMatch(String regex, String string)
   {
      if (empty(regex) || empty(string))
         return false;

      return string.matches(regex);
   }

   /**
    * Converts a * and ? wildcard style patterns into regex style pattern
    *
    * @see http://www.rgagnon.com/javadetails/java-0515.html
    * @param wildcard
    * @return
    */
   public static String wildcardToRegex(String wildcard)
   {
      wildcard = wildcard.replace("**", "*");
      StringBuffer s = new StringBuffer(wildcard.length());
      s.append('^');
      for (int i = 0, is = wildcard.length(); i < is; i++)
      {
         char c = wildcard.charAt(i);
         switch (c)
         {
            case '*':
               s.append(".*");
               break;
            case '?':
               s.append(".");
               break;
            // escape special regexp-characters
            case '(':
            case ')':
            case '[':
            case ']':
            case '$':
            case '^':
            case '.':
            case '{':
            case '}':
            case '|':
            case '\\':
               s.append("\\");
               s.append(c);
               break;
            default :
               s.append(c);
               break;
         }
      }
      s.append('$');
      return (s.toString());
   }

   public static LinkedHashMap<String, String> parseQueryString(String query)
   {
      LinkedHashMap params = new LinkedHashMap();
      try
      {
         while (query.startsWith("?") || query.startsWith("&") || query.startsWith("="))
         {
            query = query.substring(1);
         }

         if (query.length() > 0)
         {
            String[] pairs = query.split("&");
            for (String pair : pairs)
            {
               pair = pair.trim();

               if (pair.length() == 0)
                  continue;

               int idx = pair.indexOf("=");
               if (idx > 0)
               {
                  String key = pair.substring(0, idx).trim();
                  key = URLDecoder.decode(key, "UTF-8");

                  String value = pair.substring(idx + 1).trim();
                  value = URLDecoder.decode(value, "UTF-8");

                  params.put(key, value);
               }
               else
               {
                  params.put(URLDecoder.decode(pair, "UTF-8"), null);
               }
            }
         }
      }
      catch (Exception ex)
      {
         rethrow(ex);
      }
      return params;
   }

   /**
    * @param name - name to look for in sysprops and envprops
    * @param defaultValue - will be returned if prop not found
    * @return first not null of sysprop(name) || envprop(name) || defaultValue
    */
   public static String getSysEnvPropStr(String name, Object defaultValue)
   {
      Object obj = getSysEnvProp(name, defaultValue);
      if (obj != null)
         return obj.toString();
      return null;
   }

   public static int getSysEnvPropInt(String name, Object defaultValue)
   {
      Object obj = getSysEnvProp(name, defaultValue);
      if (obj != null)
         return Integer.parseInt(obj.toString());
      return -1;
   }

   public static boolean getSysEnvPropBool(String name, Object defaultValue)
   {
      Object obj = getSysEnvProp(name, defaultValue);
      if (obj != null)
         return "true".equalsIgnoreCase(obj.toString());
      return false;
   }

   /**
    * @param name - name to look for in sysprops and envprops if 'value' is null;
    * @param defaultValue - will be returned if not found on sys or env
    * @return first not null of sysprop(name) || envprop(name) || 'defaultValue'
    */
   public static Object getSysEnvProp(String name, Object defaultValue)
   {
      Object value = getProperty(name);

      return null == value ? defaultValue : value;
   }

   private static Object getProperty(String name)
   {
      String value = System.getProperty(name);

      if (Utils.empty(value))
         // try replacing dot for underscores, since Lambda doesn't support dots in env vars
         value = System.getProperty(name.replace(".", "_"));

      if (Utils.empty(value))
         value = System.getenv(name);

      if (Utils.empty(value))
         // try replacing dot for underscores, since Lambda doesn't support dots in env vars
         value = System.getenv(name.replace(".", "_"));

      if (Utils.empty(value))
      {
         InputStream stream = findInputStream(".env");
         if (stream != null)
         {
            Properties p = new Properties();
            try
            {
               p.load(stream);
               Utils.close(stream);
               value = p.getProperty(name);
            }
            catch (Exception ex)
            {
               Utils.rethrow(ex);
            }
         }
      }
      return value;
   }

   public static Object cast(String type, Object value)
   {
      try
      {
         if (value == null)
            return null;

         if (type == null)
         {
            try
            {
               if (value.toString().indexOf(".") < 0)
               {
                  return Long.parseLong(value.toString());
               }
               else
               {
                  return Double.parseDouble(value.toString());
               }
            }
            catch (Exception ex)
            {
               //must not have been an number
            }
            return value.toString();
         }

         switch (type.toLowerCase())
         {
            case "s":
            case "string":
            case "char":
            case "nchar":
            case "varchar":
            case "nvarchar":
            case "longvarchar":
            case "longnvarchar":
               return value.toString();
            case "clob":
               return value.toString().trim();
            case "n":
            case "number":
            case "numeric":
            case "decimal":
               if (value.toString().indexOf(".") < 0)
                  return Long.parseLong(value.toString());
               else
                  return Double.parseDouble(value.toString());

            case "bool":
            case "boolean":
            case "bit":
            {
               if ("1".equals(value))
                  value = "true";
               else if ("0".equals(value))
                  value = "false";

               return Boolean.parseBoolean(value.toString());
            }

            case "tinyint":
               return Byte.parseByte(value.toString());
            case "smallint":
               return Short.parseShort(value.toString());
            case "integer":
               return Integer.parseInt(value.toString());
            case "bigint":
               return Long.parseLong(value.toString());
            case "float":
            case "real":
            case "double":
               return Double.parseDouble(value.toString());
            case "datalink":
               return new URL(value.toString());

            case "binary":
            case "varbinary":
            case "longvarbinary":
               throw new UnsupportedOperationException("Binary types are currently unsupporrted");

            case "date":
            case "datetime":
               return new java.sql.Date(date(value.toString()).getTime());
            case "timestamp":
               return new java.sql.Timestamp(date(value.toString()).getTime());

            case "array":

               if (value instanceof JSArray)
                  return value;
               else
                  return JSNode.parseJsonArray(value + "");

            case "object":

               if (value instanceof JSNode)
                  return value;
               else
               {
                  String json = value.toString().trim();
                  if (json.length() > 0)
                  {
                     char c = json.charAt(0);
                     if (c == '[' || c == '{')
                        return JSNode.parseJson(value + "");
                  }
                  return json;
               }

            default :
               ApiException.throw500InternalServerError("Error casting '%s' as type '%s'", value, type);
         }
      }
      catch (Exception ex)
      {
         Utils.rethrow(ex);
         //throw new RuntimeException("Error casting '" + value + "' as type '" + type + "'", ex);
      }

      return null;
   }

   /**
    * Utility to call a close() method on supplied objects if it exists and completely ignore any exceptions
    * @param toClose
    */
   public static void close(Object... toClose)
   {
      for (Object o : toClose)
      {
         if (o != null)
         {
            try
            {
               if (o instanceof Closeable)
               {
                  ((Closeable) o).close();
               }
               else
               {
                  Method m = o.getClass().getMethod("close");
                  if (m != null)
                  {
                     m.invoke(o);
                  }
               }
            }
            catch (NoSuchMethodException nsme)
            {
               //nsme.printStackTrace();
            }
            catch (Exception ex)
            {
               //ex.printStackTrace();
            }
         }
      }
   }

}