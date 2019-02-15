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
package io.rocketpartners.cloud.utils;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ISO8601Utils;

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
   
   
   public static JSArray parseJsonArray(String json)
   {
      return ((JSArray) parseJson(json));
   }

   public static JSObject parseJsonObject(String json)
   {
      return ((JSObject) parseJson(json));
   }

   static Object parseJson(String json)
   {
      try
      {
         ObjectMapper mapper = new ObjectMapper();
         JsonNode rootNode = mapper.readValue(json, JsonNode.class);

         Object parsed = map(rootNode);
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

   /**
    * @see https://stackoverflow.com/questions/14028716/how-to-remove-control-characters-from-java-string
    * @param str
    * @return
    */
   public static String encodeJson(String str)
   {
      if (str == null)
         return null;

      str = str.replaceAll("[\\p{Cntrl}\\p{Cc}\\p{Cf}\\p{Co}\\p{Cn}\u00A0&&[^\r\n\t]]", " ");
      return str;
   }

   static Object map(JsonNode json)
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
            retVal.add(map(child));
         }

         return retVal;
      }
      else if (json.isObject())
      {
         JSObject retVal = null;
         retVal = new JSObject();

         Iterator<String> it = json.fieldNames();
         while (it.hasNext())
         {
            String field = it.next();
            JsonNode value = json.get(field);
            retVal.put(field, map(value));
         }
         return retVal;
      }

      throw new RuntimeException("unparsable json:" + json);
   }
   
   public static int roundUp(int num, int divisor)
   {
      int sign = (num > 0 ? 1 : -1) * (divisor > 0 ? 1 : -1);
      return sign * (Math.abs(num) + Math.abs(divisor) - 1) / Math.abs(divisor);
   }

   /**
    * Faster and null safe way to call Integer.parseInt(str.trim()) that swallows exceptions.
    */
   public static int atoi(String str)
   {
      try
      {
         return Integer.parseInt(str.trim());
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
   public static long atol(String str)
   {
      try
      {
         return Long.parseLong(str.trim());
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
   public static float atof(String str)
   {
      try
      {
         return Float.parseFloat(str.trim());
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
   public static double atod(String str)
   {
      try
      {
         return Double.parseDouble(str.trim());
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

   public static Date parseIso8601(String date)throws ParseException
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

   /**
    * Same as calling Class.getMethod but it returns null intead of throwing a NoSuchMethodException
    * @param clazz
    * @param name
    * @param args
    * @return
    */
   public static Method getMethod(Class clazz, String name, Class... args)
   {
      try
      {
         return clazz.getMethod(name, args);
      }
      catch (NoSuchMethodException ex)
      {

      }
      return null;
   }

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
                  System.out.println("skipping: " + f);
               }
            }
         }
         clazz = clazz.getSuperclass();
      }
      while (clazz != null && !Object.class.equals(clazz));

      return fields;
   }

   /**
    * Finds the Field in the inheritance heirarchy and sets it
    * @param name
    * @param value
    * @param o
    * @throws NoSuchFieldException
    * @throws IllegalAccessException
    */
   public static void setField(String name, Object value, Object o) throws NoSuchFieldException, IllegalAccessException
   {
      Field f = getField(name, o.getClass());
      f.setAccessible(true);
      f.set(o, value);
   }

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
    * @param clazz
    * @param name
    * @return all methods in the inheritance hierarchy with the given name
    */
   public static List<Method> getMethods(Class clazz, String name)
   {
      List<Method> methods = new ArrayList();

      do
      {
         for (Method m : clazz.getMethods())
         {
            if (m.getName().equalsIgnoreCase(name))
               methods.add(m);
         }
      }
      while (clazz != null && !Object.class.equals(clazz));

      return methods;
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
    * Tries to find a bean property getter then tries Field value, then defaults to the supplied defaultVal
    * @param name
    * @param object
    * @param defaultVal
    * @return
    */
   public static Object getProperty(String name, Object object, Object defaultVal)
   {
      Object value = getProperty(name, object);
      if (empty(value))
      {
         value = defaultVal;
      }

      return value;
   }

   /**
    * A best effort field by field shallow copier that will ignore all errors. This does not recurse.
    * @param src
    * @param dest
    */
   public static void copyFields(Object src, Object dest)
   {
      List<Field> fields = getFields(src.getClass());
      for (Field f : fields)
      {
         try
         {
            Object value = f.get(src);
            setField(f.getName(), value, dest);
         }
         catch (Exception ex)
         {
         }
      }
   }

   /**
    * Utility to call a close() method on supplied objects if it exists and completely ignore any errors
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

   /**
    * Copy all data from src to dst and close the reader/writer
    * @param src
    * @param dest
    * @throws Exception
    */
   public static void pipe(Reader src, Writer dest) throws Exception
   {
      try
      {
         char buffer[] = new char[K64];
         int len = buffer.length;
         synchronized (src)
         {
            while (true)
            {
               len = src.read(buffer);
               if (len == -1)
                  break;
               dest.write(buffer, 0, len);
            }
         }
      }
      finally
      {
         flush(dest);
         close(src);
         close(dest);
      }
   }

   public static File createTempFile(File file) throws IOException
   {
      if (file == null)
         return createTempFile("working.tmp");
      else
         return createTempFile(file.getName());
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
    * Simply calls stream.flush() but throws RuntimeException instead of IOException
    * @param stream
    */
   public static void flush(Flushable stream)
   {
      try
      {
         if (stream != null)
         {
            stream.flush();
         }
      }
      catch (Exception ex)
      {
         rethrow(ex);
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
            fileOrUrl = URLDecoder.decode(fileOrUrl);
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
    * Attempts to locate the stream as a file, url, or classpath resource and then reads it all as a string
    * @param fileOrUrl
    * @return
    * @throws Exception
    */
   public static String read(String fileOrUrl) throws Exception
   {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      InputStream in = findInputStream(fileOrUrl);
      pipe(in, out);
      return new String(out.toByteArray());
   }

   /**
    * Recursively deletes the file or directory
    * @param file
    * @return
    */
   public static boolean delete(File file)
   {
      boolean deleted = true;
      if (file != null && file.exists())
      {
         if (file.isDirectory())
         {
            for (File f : file.listFiles())
            {
               deleted &= delete(f);
            }
         }
         deleted &= file.delete();
      }
      return deleted;
   }

   /**
    * Copies the given file or recursively copies a directory
    * @param src
    * @param dst
    */
   public static void copy(File src, File dst)
   {
      if (src.isFile())
      {
         copyFile(src, dst);
      }
      else
      {
         copyDir(src, dst);
      }
   }

   public static boolean copy(File srcDir, File srcFile, File dstDir)
   {
      try
      {
         String dest = srcFile.getCanonicalPath();
         dest = dest.substring(srcDir.getCanonicalPath().length(), dest.length());
         if (dest.startsWith("/") || dest.startsWith("\\"))
         {
            dest = dest.substring(1, dest.length());
         }

         File dstFile = new File(dstDir, dest);
         return copyFile(srcFile, dstFile);
      }
      catch (Exception ex)
      {
         rethrow(ex);
      }
      return false;
   }

   protected static void copyDir(File srcDir, File dstDir)
   {
      File[] files = srcDir.listFiles();
      for (int i = 0; files != null && i < files.length; i++)
      {
         copy(srcDir, files[i], dstDir);
      }
   }

   protected static boolean copyFile(File srcFile, File dstFile)
   {
      FileInputStream fis = null;
      FileOutputStream fos = null;
      FileChannel sourceChannel = null;
      FileChannel destinationChannel = null;

      try
      {
         if (!dstFile.getParentFile().exists())
         {
            dstFile.getParentFile().mkdirs();
         }
         else
         {
            if (dstFile.exists() && dstFile.lastModified() >= srcFile.lastModified())
            {
               return false;
            }
         }

         fis = new FileInputStream(srcFile);
         fos = new FileOutputStream(dstFile);
         sourceChannel = fis.getChannel();
         destinationChannel = fos.getChannel();
         destinationChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
         sourceChannel.close();
         destinationChannel.close();
         fis.close();
         fos.close();

         dstFile.setLastModified(srcFile.lastModified());
      }
      catch (Exception ex)
      {
         return false;
      }
      finally
      {
         if (sourceChannel != null)
         {
            try
            {
               sourceChannel.close();
            }
            catch (Exception ex)
            {

            }
         }

         if (destinationChannel != null)
         {
            try
            {
               destinationChannel.close();
            }
            catch (Exception ex)
            {

            }
         }

         if (fis != null)
         {
            try
            {
               fis.close();
            }
            catch (Exception ex)
            {

            }
         }

         if (fos != null)
         {
            try
            {
               fos.close();
            }
            catch (Exception ex)
            {

            }
         }
      }
      return true;
   }

   public static String substring(String string, String regex, int group)
   {
      Pattern p = Pattern.compile(regex);
      Matcher m = p.matcher(string);
      if (m.find())
         return m.group(group);

      return null;
   }

   public static String getShortCause(Throwable t)
   {
      return getShortCause(t, 15);
   }

   public static String getShortCause(Throwable t, int lines)
   {
      t = getCause(t);
      //return System.getProperty("line.separator") + limitLines(clean(getStackTraceString(t)), lines);
      return limitLines(clean(getStackTraceString(t)), lines);
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

   public static String getStackTraceString(Thread t)
   {
      return getStackTraceString(t, t.getStackTrace());
   }

   public static String getStackTraceString(Thread t, StackTraceElement[] stackTrace)
   {
      StringBuffer buff = new StringBuffer();

      buff.append("Thread -------------------------").append("\r\n");
      buff.append("  id    = ").append(t.getId()).append("\r\n");
      buff.append("  name  = ").append(t.getName()).append("\r\n");
      buff.append("  state = ").append(t.getState()).append("\r\n");
      buff.append("  trace: ").append("\r\n");
      for (int i = 0; i < stackTrace.length; i++)
      {
         buff.append("\tat").append(stackTrace[i]).append("\r\n");
      }

      return buff.toString();
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

   /** Get the class from a line on the stack trace line. */

   public static String getMethodNameFromStackLine(String line)
   {
      if (line != null)
      {
         line = line.trim();
         int pos = line.indexOf("at ");
         if (pos == 0)
         {
            line = line.substring(3);
            pos = line.indexOf('(');
            if (pos < 0)
            {
               pos = line.indexOf(' ');
            }
            if (pos > 0)
            {
               String clsStr = line.substring(0, pos);
               clsStr = clsStr.trim();

               pos = clsStr.lastIndexOf('.');
               String methodName = clsStr.substring(pos + 1);

               return methodName;
            }
         }
      }

      return null;
   }

   public static Class getClassFromStackLine(String line)
   {
      if (line != null)
      {
         line = line.trim();
         int pos = line.indexOf("at ");
         if (pos == 0)
         {
            line = line.substring(3);
            pos = line.indexOf('(');
            if (pos < 0)
            {
               pos = line.indexOf(' ');
            }
            if (pos > 0)
            {
               String clsStr = line.substring(0, pos);
               clsStr = clsStr.trim();

               pos = clsStr.lastIndexOf('.');
               clsStr = clsStr.substring(0, pos);

               try
               {
                  return Class.forName(clsStr);
               }
               catch (ClassNotFoundException e)
               {
               }
            }
         }
      }
      return null;
   }

   static String clean(String stackTrace)
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
         return clean(buffer.toString());
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

}