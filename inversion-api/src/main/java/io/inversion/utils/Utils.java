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

import com.fasterxml.jackson.databind.util.ISO8601Utils;
import io.inversion.ApiException;
import io.inversion.Request;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Collection of utility methods designed to make
 * java programming less verbose
 */
public class Utils {

    static final String   NEW_LINE                   = System.getProperty("line.separator");
    static final String[] EMPTY_STRING_ARRAY         = new String[0];
    static final String   CONTAINS_TOKEN_PLACEHOLDER = "INVERSIONREPLACEDINVERSION";
    static final Pattern  CONTAINS_TOKEN_PATTERN     = Pattern.compile("\\b\\Q" + CONTAINS_TOKEN_PLACEHOLDER + "\\E\\b", Pattern.CASE_INSENSITIVE);

    /**
     * A null safe loose equality checker.
     *
     * @param obj1 an object
     * @param obj2 an object to compare to obj1
     * @return true when args are strictly equal or toString equal
     */
    public static boolean equal(Object obj1, Object obj2) {
        if (obj1 == obj2)
            return true;

        if (obj1 == null || obj2 == null)
            return false;

        if (obj1.equals(obj2))
            return true;

        return obj1.toString().equals(obj2.toString());
    }

    /**
     * Checks to see if <code>toFind</code> is in <code>values</code> array using loose equality checking
     *
     * @param toFind the object to find
     * @param values where to try and find it
     * @return true if toFind is loosely equal to any of <code>values</code>
     */
    public static boolean in(Object toFind, Object... values) {
        for (Object val : values) {
            if (equal(toFind, val))
                return true;
        }
        return false;
    }

    /**
     * @param arr an array of objects to check and see if any are not empty
     * @return true if any args are not null with a toString().length() @gt; 0
     */
    public static boolean empty(Object... arr) {
        boolean empty = true;
        for (int i = 0; empty && arr != null && i < arr.length; i++) {
            Object obj = arr[i];
            if (obj != null && obj.toString().length() > 0)
                empty = false;
        }
        return empty;
    }

    public static Object first(List list) {
        if (list.size() > 0)
            return list.get(0);
        return null;
    }

    public static Object last(List list) {
        if (list.size() > 0)
            return list.get(list.size() - 1);
        return null;
    }


    /**
     * Fluent override of Collections.addAll()
     *
     * @param collection  the collection to add items to
     * @param items the items to add
     * @param <T>   a subclass of Collection
     * @return the collection passed in
     * @see Collections#addAll(Collection, Object[])
     */
    @SuppressWarnings("unchecked")
    public static <T extends Collection> T add(T collection, Object... items) {
        if (items != null)
            Collections.addAll(collection, items);
        return collection;
    }

    /**
     * String.endsWith surrogate for StringBuffer and StringBuilder
     *
     * @param seq the string to check
     * @param end the ending to check for
     * @return true if seq ends with end
     */
    public static boolean endsWith(CharSequence seq, String end) {
        if (end == null)
            return false;

        int seqLen = seq.length();
        int endLen = end.length();

        if (seqLen < endLen)
            return false;

        for (int i = 1; i <= endLen; i++) {
            char s = seq.charAt(seqLen - i);
            char e = end.charAt(endLen - i);
            if (s != e)
                return false;
        }
        return true;
    }

    /**
     * String.startsWith surrogate for StringBuffer and StringBuilder
     *
     * @param seq   the string to check
     * @param start the starting substring to check for
     * @return true if seq ends with end
     */
    public static boolean startsWith(CharSequence seq, String start) {
        if (start == null)
            return false;

        int seqLen   = seq.length();
        int startLen = start.length();

        if (seqLen < startLen)
            return false;

        for (int i = 0; i < startLen; i++) {
            char s = seq.charAt(seqLen);
            char e = start.charAt(startLen);
            if (s != e)
                return false;
        }
        return true;
    }

    /**
     * Concatenates non empty <code>pieces</code> separated by <code>glue</code> and
     * intelligently flattens collections.
     *
     * @param glue   the joining string
     * @param pieces the pieces to join
     * @return a concatenation of pieces separated by glue
     */
    public static String implode(String glue, Object... pieces) {
        if (pieces != null && pieces.length == 1 && pieces[0] instanceof Collection)
            pieces = ((Collection) pieces[0]).toArray();

        StringBuilder str = new StringBuilder();
        for (int i = 0; pieces != null && i < pieces.length; i++) {
            if (pieces[i] != null) {
                String piece = pieces[i] instanceof Collection ? implode(glue, pieces[i]) : pieces[i].toString();

                if (piece.length() > 0) {
                    List<String> subPieces = explode(glue, piece);

                    for (String subPiece : subPieces) {
                        if (subPiece.length() > 0) {
                            if (str.length() > 0)
                                str.append(glue);
                            str.append(subPiece);
                        }
                    }
                }
            }
        }
        return str.toString();
    }

    /**
     * Similar to String.split but trims whitespace and excludes empty strings
     *
     * @param delimiter the split delimiter
     * @param pieces    the strings to split
     * @return all non empty strings from all pieces
     */
    public static List<String> explode(String delimiter, String... pieces) {
        if (".".equals(delimiter))
            delimiter = "\\.";

        List<String> exploded = new ArrayList<>();
        for (int i = 0; pieces != null && i < pieces.length; i++) {
            if (Utils.empty(pieces[i]))
                continue;

            String[] parts = pieces[i].split(delimiter);
            for (String s : parts) {
                String part = s.trim();
                if (!Utils.empty(part)) {
                    exploded.add(part);
                }
            }
        }

        return exploded;
    }

    /**
     * Breaks the string on <code>splitOn</code> but not when inside a <code>quoteChars</code>
     * quoted string.
     *
     * @param string     the string to split
     * @param splitOn    the character to split on
     * @param quoteChars quote chars that invalidate the instance of slit char
     * @return the split parts
     */
    public static List<String> split(String string, char splitOn, char... quoteChars) {
        List<String>   strings = new ArrayList<>();
        Set<Character> quotes  = new HashSet<>();
        for (char c : quoteChars)
            quotes.add(c);

        boolean       quoted = false;
        StringBuilder buff   = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);

            if (c == splitOn && !quoted) {
                if (buff.length() > 0) {
                    strings.add(buff.toString());
                    buff = new StringBuilder();
                }
                continue;
            } else if (quotes.contains(c)) {
                quoted = !quoted;
            }

            buff.append(c);
        }
        if (buff.length() > 0)
            strings.add(buff.toString());

        return strings;
    }

    public static String substringBefore(String string, String breakBefore) {
        int idx = string.indexOf(breakBefore);
        if (idx > -1)
            string = string.substring(0, idx);

        return string;
    }

    public static String substringAfter(String string, String breakAfterLast) {
        int idx = string.lastIndexOf(breakAfterLast);
        if (idx > -1)
            string = string.substring(idx + breakAfterLast.length());

        return string;
    }

    /**
     * A heroically forgiving message string formatter.
     * <p>
     * This method attempts to safely toString all of the args and replaces
     * any "{}" characters with "%s" before formatting via String.format(String, Object[]).
     * <p>
     * Any Throwables in the args list will have their short cause string appended to the end
     * of the formatted message.
     * <p>
     * If the format is invalid or contains too few or too many args, the method will
     * make sure that all arg toStrings are in the output.
     * <p>
     * The goal here is to make sure that no matter what happens, you will get something
     * useful out of this message if not exactly to the format spec.
     *
     * @param format a string containing "{}" arg placeholders of formatted per java.util.Formatter
     * @param args objects that will be replaced into their <code>format</code> placeholders.
     * @return the formatted string
     */
    public static String format(String format, Object... args) {

        if(format == null)
            format = "";

        if(args != null && args.length == 1 && args[0].getClass().isArray())
        {
            Object arg0 = args[0];
            int size = Array.getLength(arg0);
            args = new Object[size];
            for(int i=0; i<size; i++) {
                try {
                    args[i] = Array.get(arg0, i);
                }
                catch(Exception ex) {
                    args[i] = ex.getMessage();
                }
            }
        }

        if(args == null || args.length < 1)
            return format;

        format = format.trim();
        if(format.isEmpty())
        {
            for(int i=0; i<args.length; i++) {
                format += "{}";
                if(i<args.length-1)
                    format += ", ";
            }
        }

        List<String> errors = new ArrayList();

        try {

            for(int i=0; i<args.length; i++) {
                try{
                    if(args[i] == null) {
                        args[i] = "null";
                    }
                    else if (args[i] instanceof Throwable) {
                        String cause = Utils.getShortCause((Throwable)args[i]);
                        if(cause == null)
                            cause = "UNKNOWN NULL CAUSE";

                        String message = cause;
                        if(cause.indexOf("\n") > 1){
                            message = cause.substring(0, cause.indexOf("\n")).trim();
                        }
                        errors.add(cause);
                        args[i] = message;
                    }
                    else if (args[i] instanceof byte[]) {
                        args[i] = new String((byte[]) args[i]);
                    }
                    else if (args[i].getClass().isArray()) {
                        args[i] = "[" + format(null, args[i]) + "]";
                    }
                    else{
                        //the objects to string could throw an error
                        args[i] = args[i] + "";
                    }
                }
                catch(Exception ex) {
                    args[i] = "ERROR: " + ex.getMessage();
                }
            }

            //-- most logging frameworks are using "{}" to indicate
            //-- var placeholders these days
            format = format.replace("{}", "%s");
            format = String.format(format, args);

            //the user could have supplied more args than there were {} so be friendly and add them to the output
            for(int i=0; i<args.length; i++)
            {
                String arg = (String) args[i];
                if(format.indexOf(arg) < 0)
                    format += ", " + arg;
            }
        }
        catch(Exception ex)
        {
            //probably a format error or incorrect number of args...attempt to do something useful anyway and not error
            for(int i=0; args != null && i<args.length; i++)
            {
                try{
                    format += ", {" + args[i] + "}";
                }
                catch(Exception ex2){
                    format += ", {" + ex2.getMessage() + "}";
                }
            }
        }
        for(String error : errors)
            format += "\r\n" + error;

        return format;
    }



    public static ArrayListValuedHashMap addToMap(ArrayListValuedHashMap<String, String> multiMap, String... kvPairs) {
        if (kvPairs != null && kvPairs.length % 2 > 0)
            throw new RuntimeException("kvPairs.length must be evenly divisible by 2.");

        for (int i = 0; kvPairs != null && i < kvPairs.length - 1; i += 2)
            multiMap.put(kvPairs[i], kvPairs[i + 1]);

        return multiMap;
    }

    public static <M extends Map<String, String>> M addToMap(M map, String... keyValuePairs) {
        if (keyValuePairs != null && keyValuePairs.length % 2 > 0)
            throw new RuntimeException("keyValuePairs.length must be evenly divisible by 2.");

        for (int i = 0; keyValuePairs != null && i < keyValuePairs.length - 1; i += 2)
            map.put(keyValuePairs[i], keyValuePairs[i + 1]);

        return map;
    }

    /**
     * Similar to Arrays.asList but with raw ArrayList return type assuming that
     * all objects don't have to be the same type.
     *
     * @param objects the objects to add
     * @return a new ArrayList containing <code>objects</code>
     */
    public static ArrayList asList(Object... objects) {
        ArrayList<Object> list = new ArrayList<>(objects != null ? objects.length : 0);
        if (objects != null) {
            Collections.addAll(list, objects);
        }
        return list;
    }

    /**
     * Similar to Arrays.asList but returning a raw HashSet assuming that
     * all objects don't have to be the same type.
     *
     * @param objects the objects to add
     * @return a new HashSet containing <code>objects</code>
     */
    public static HashSet asSet(Object... objects) {
        HashSet<Object> set = new HashSet<>();
        if (objects != null) {
            Collections.addAll(set, objects);
        }
        return set;
    }

    /**
     * Adds each even and old object as a key/value pair to a HashMap
     *
     * @param keyValuePairs a list of key/value pairs that should have an even number of elements
     * @return a new HashMap containing keyValuePairs
     */
    @SuppressWarnings("unchecked")
    public static HashMap asMap(Object... keyValuePairs) {
        HashMap map = new HashMap<>();
        for (int i = 0; keyValuePairs != null && i < keyValuePairs.length - 1; i += 2) {
            map.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return map;
    }

    /**
     * Checks for a whole word case insensitive match of <code>findThisToken</code>
     * in <code>inThisString</code>
     * <p>
     * https://www.baeldung.com/java-regexp-escape-char
     * https://stackoverflow.com/questions/7459263/regex-whole-word
     *
     * @param findThisToken the string to find
     * @param inThisString  in this other string
     * @return true if findThisToken exists as a whole world in inThisString
     */
    public static boolean containsToken(String findThisToken, String inThisString) {
        if(inThisString == null)
            return findThisToken == null;

        findThisToken = findThisToken.toLowerCase();
        inThisString = inThisString.toLowerCase();
        //-- this replacement is done so that <code>findThisToken</code> can itself contain
        //-- special chars and word brakes etc that will throw off the whole word regex.
        inThisString = inThisString.replace(findThisToken, CONTAINS_TOKEN_PLACEHOLDER);
        return CONTAINS_TOKEN_PATTERN.matcher(inThisString).find();
    }

    /**
     * Removes all matching pairs of '"` characters from the
     * start and end of a string.
     *
     * @param str the string to dequote
     * @return str with matched pairs of leading/trailing '"` characters removed
     */
    public static String dequote(String str) {
        return dequote(str, new char[]{'\'', '"', '`'});
    }

    /**
     * Removes all matching pairs of leading/trailing <code>quoteChars</code> from the start and end of a string.
     *
     * @param str        the string to dequote
     * @param quoteChars characters to treat as quotes
     * @return str with matched pairs of leading/trailing quoteChars removed
     */
    public static String dequote(String str, char[] quoteChars) {
        if (str == null)
            return null;

        while (str.length() >= 2 && str.charAt(0) == str.charAt(str.length() - 1))// && (str.charAt(0) == '\'' || str.charAt(0) == '"' || str.charAt(0) == '`'))
        {
            boolean changed = false;
            for (char quoteChar : quoteChars) {
                if (str.charAt(0) == quoteChar) {
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
     * 12.3334 -@gt; 12.33
     * 23.0 -@gt; 23.00
     * 45.677 -@gt; 45.68
     *
     * @param amount the amount to round
     * @return the amount rounded to two decimal places
     */
    public static BigDecimal toDollarAmount(double amount) {
        return new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
    }

    public static int roundUp(int num, int divisor) {
        int sign = (num > 0 ? 1 : -1) * (divisor > 0 ? 1 : -1);
        return sign * (Math.abs(num) + Math.abs(divisor) - 1) / Math.abs(divisor);
    }

    /**
     * Converst a string to a boolean.
     * <p>
     * Easier and null safe way to call Boolean.parseBoolean(str.trim()) that swallows exceptions.
     *
     * @param str the string to parse as a boolean
     * @return true if the trimmed lower case str is "0" or "false"
     */
    public static boolean atob(Object str) {
        try {
            String bool = (str + "").trim().toLowerCase();
            return !("0".equals(bool) || "false".equals(bool));
        } catch (Exception ex) {
            //ignore
        }
        return false;
    }

    /**
     * Convert a string to an integer.
     * <p>
     * Easier null safe way to call Integer.parseInt(str.trim()) that swallows exceptions.
     *
     * @param str the string to parse
     * @return the parsed value or -1 if the string does not parse
     */
    public static int atoi(Object str) {
        try {
            return Integer.parseInt(str.toString().trim());
        } catch (Exception ex) {
            //ignore
        }
        return -1;
    }

    /**
     * Convert a string to a long.
     * <p>
     * Easier null safe way to call Long.parseLong(str.trim()) that swallows exceptions.
     *
     * @param str the string to parse
     * @return the parsed value or -1 if the string does not parse
     */
    public static long atol(Object str) {
        try {
            return Long.parseLong(str.toString().trim());
        } catch (Exception ex) {
            //ignore
        }
        return -1;
    }

    /**
     * Convert a string to a float.
     * <p>
     * Easier null safe way to call Float.parseFloat(str.trim()) that swallows exceptions.
     *
     * @param str the string to parse
     * @return the parsed value or -1 if the string does not parse
     */
    public static float atof(Object str) {
        try {
            return Float.parseFloat(str.toString().trim());
        } catch (Exception ex) {
            //ignore
        }
        return -1;
    }

    /**
     * Convert a string to a double.
     * <p>
     * Easier null safe way to call Double.parseDouble(str.trim()) that swallows exceptions.
     *
     * @param str the string to parse
     * @return the parsed value or -1 if the string does not parse
     */
    public static double atod(Object str) {
        try {
            return Double.parseDouble(str.toString().trim());
        } catch (Exception ex) {
            //ignore
        }
        return -1;
    }

    /**
     * Creates a lowercase url safe string.
     *
     * @param str the string to slugify
     * @return the slugified string
     */
    public static String slugify(String str) {
        if (str == null) {
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
     * @param bytes the bytes to hash
     * @return Hash the bytes with SHA-1
     */
    public static String sha1(byte[] bytes) {
        return hash(bytes, "SHA-1");
    }

    /**
     * @param bytes the bytes to hash
     * @return Hash the bytes with MD5
     */
    public static String md5(byte[] bytes) {
        return hash(bytes, "MD5");
    }

    /**
     * @param bytes     the bytes to hash
     * @param algorithm the hash algorithm
     * @return Hash the bytes with the given algorithm
     */
    public static String hash(byte[] bytes, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            digest.update(bytes);
            bytes = digest.digest();
            return (new HexBinaryAdapter()).marshal(bytes);
        } catch (Exception ex) {
            rethrow(ex);
        }
        return null;
    }

    /**
     * Less typing to call System.currentTimeMillis()
     *
     * @return the current time in milliseconds
     */
    public static long time() {
        return System.currentTimeMillis();
    }

    public static Date parseIso8601(String date) {
        //return ISO8601Utils.parse(date, new ParsePosition(0));
        DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
        TemporalAccessor  accessor      = timeFormatter.parse(date);
        return Date.from(Instant.from(accessor));
    }

    public static String formatIso8601(Date date) {
        return ISO8601Utils.format(date);
        //return DateTimeFormatter.ISO_DATE_TIME.format(date.toInstant());
    }

    /**
     * Simple one liner to avoid verbosity of using SimpleDateFormat
     *
     * @param date   the date to format
     * @param format the format
     * @return the formatted date
     */
    public static String formatDate(Date date, String format) {
        SimpleDateFormat f = new SimpleDateFormat(format);
        return f.format(date);
    }

    /**
     * Faster way to apply a SimpleDateFormat without having to catch ParseException
     *
     * @param date   the date string to format
     * @param format the format string
     * @return the formatted date
     */
    public static Date date(String date, String format) {
        try {
            date = date.trim();
            SimpleDateFormat df = new SimpleDateFormat(format);
            return df.parse(date);
        } catch (Exception ex) {
            rethrow(ex);
        }
        return null;
    }

    /**
     * Attempts to parse a date with several usual formats.
     * <p>
     * Formats attempted:
     * <ol>
     *  <li>an ISO8601 data
     *  <li>then yyyy-MM-dd
     *  <li>then MM/dd/yy
     *  <li>then MM/dd/yyyy
     *  <li>then yyyyMMdd
     * </ol>
     *
     * @param date the date string to parse
     * @return the parsed date
     * @see #castJsonInput(String, Object)
     */
    public static Date date(String date) {
        try {
            return parseIso8601(date);
//            DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
//            TemporalAccessor  accessor      = timeFormatter.parse(date);
//            return Date.from(Instant.from(accessor));
        } catch (Exception ex) {
            //do nothing
        }
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
            return f.parse(date);

        } catch (Exception ex) {
            //do nothing
        }

        try {
            SimpleDateFormat f = new SimpleDateFormat("MM/dd/yy");

            int lastSlash = date.lastIndexOf("/");
            if (lastSlash > 0 && lastSlash == date.length() - 5) {
                f = new SimpleDateFormat("MM/dd/yyyy");
            }
            return f.parse(date);

        } catch (Exception ex) {
            //do nothing
        }

        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd");
            return f.parse(date);
        } catch (Exception ex) {
            throw new RuntimeException("unsupported format: " + date);
        }

    }

    public static boolean testCompare(String expected, String actual) {
        expected = expected != null ? expected : "";
        actual = actual != null ? actual : "";

        expected = expected.replaceAll("\\s+", " ").trim();
        actual = actual.replaceAll("\\s+", " ").trim();

        if (!expected.equals(actual)) {
            System.out.println("EXPECTED : " + expected);
            System.out.println("ACTUAL   : " + actual);
            for (int i = 0; i < expected.length() && i < actual.length(); i++) {
                if (expected.charAt(i) == actual.charAt(i)) {
                    System.out.print("           ");

                } else {
                    System.out.println("X");
                    break;
                }
            }
            System.out.println(" ");
            return false;

        }
        return true;
    }

    /**
     * Tries to unwrap nested exceptions looking for the root cause
     *
     * @param t the error to investigate
     * @return the recursively root cause
     */
    public static Throwable getCause(Throwable t) {
        Throwable original = t;

        int guard = 0;
        while (t != null && t.getCause() != null && t.getCause() != t && guard < 100) {
            t = t.getCause();
            guard++;
        }

        if (t == null) {
            t = original;
        }

        return t;
    }

    /**
     * Shortcut for throw new RuntimeException(message);
     *
     * @param message the error message
     * @throws RuntimeException always
     */
    public static void error(String message) throws RuntimeException {
        throw new RuntimeException(message);
    }

    /**
     * Throws the root cause of <code>error</code> as a RuntimeException
     *
     * @param error error to rethrow
     * @throws RuntimeException always
     */
    public static void rethrow(Throwable error) throws RuntimeException {
        rethrow(null, error);
    }

    /**
     * Throws the root cause of e as a RuntimeException
     *
     * @param message the optional message to include in the RuntimeException
     * @param error   the error to rethrow
     * @throws RuntimeException always
     */
    public static void rethrow(String message, Throwable error) throws RuntimeException {
        Throwable cause = error;

        while (cause.getCause() != null && cause.getCause() != error)
            cause = cause.getCause();

        if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        }

        if (error instanceof RuntimeException)
            throw (RuntimeException) error;

        if (!empty(message)) {
            throw new RuntimeException(message, error);
        } else {
            throw new RuntimeException(error);
        }
    }

    /**
     * Easy way to call Thread.sleep(long) without worrying about try/catch for InterruptedException
     *
     * @param milliseconds the number of milliseconds to sleep
     * @throws RuntimeException if InterruptedException is thrown
     */
    public static void sleep(long milliseconds) throws RuntimeException {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            rethrow(e);
        }
    }

    public static String getShortCause(Throwable t) {
        return getShortCause(t, 15);
    }

    public static String getShortCause(Throwable t, int lines) {
        t = getCause(t);
        //return System.getProperty("line.separator") + limitLines(clean(getStackTraceString(t)), lines);
        return limitLines(cleanStackTrace(getStackTraceString(t)), lines);
    }

    public static List<String> getStackTraceLines(Throwable stackTrace) {
        ByteArrayOutputStream baos   = new ByteArrayOutputStream();
        PrintWriter           writer = new PrintWriter(baos);

        if (stackTrace != null) {
            stackTrace.printStackTrace(writer);
        } else {
            try {
                throw new Exception();
            } catch (Exception e) {
                e.printStackTrace(writer);
            }
        }

        writer.close();

        String   s    = new String(baos.toByteArray());
        String[] sArr = s.split("\n");
        return new ArrayList<>(Arrays.asList(sArr));
    }

    public static String getStackTraceString(Throwable stackTrace) {
        ByteArrayOutputStream baos   = new ByteArrayOutputStream();
        PrintWriter           writer = new PrintWriter(baos);

        boolean createNewTrace = false;

        if (stackTrace != null) {
            try {
                stackTrace.printStackTrace(writer);
            } catch (Exception e) {
                createNewTrace = true;
            }
        } else {
            createNewTrace = true;
        }

        if (createNewTrace) {
            try {
                throw new Exception("Unable to get original stacktrace.");
            } catch (Exception e) {
                e.printStackTrace(writer);
            }
        }

        writer.close();

        return new String(baos.toByteArray());

    }

    static String cleanStackTrace(String stackTrace) {
        String[] ignoredCauses = new String[]{//
                //
                "java.lang.reflect.UndeclaredThrowableException", //
                "java.lang.reflect.InvocationTargetException"};

        String[] lines = splitLines(stackTrace);

        boolean chop = false;
        if (stackTrace.indexOf("Caused by: ") > 0) {
            for (String ignoredCause : ignoredCauses) {
                if (lines[0].contains(ignoredCause)) {
                    chop = true;
                    break;
                }
            }
        }

        int start = 0;
        if (chop) {
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].startsWith("Caused by:")) {
                    lines[i] = lines[i].substring(10);
                    break;
                }

                start++;
            }
        }

        StringBuilder buffer = new StringBuilder();
        for (int i = start; i < lines.length; i++) {
            buffer.append(lines[i]).append("\r\n");
        }

        if (chop) {
            return cleanStackTrace(buffer.toString());
        } else {
            return buffer.toString();
        }
    }

    public static String[] splitLines(String text) {
        if (text == null || "".equals(text)) {
            return EMPTY_STRING_ARRAY;
        }

        String lineSeparator = text.contains(NEW_LINE) ? NEW_LINE : "\n";
        return text.split(lineSeparator);
    }

    public static String limitLines(String text, int limit) {
        StringBuilder buffer = new StringBuilder();
        String[]      lines  = splitLines(text);
        for (int i = 0; i < lines.length && i < limit; i++) {
            if (i == limit - 1 && i != lines.length - 1) {
                buffer.append("...").append(lines.length - i).append(" more");
            } else {
                buffer.append(lines[i]).append(NEW_LINE);
            }
        }

        return buffer.toString();
    }

    /**
     * Searches the inheritance hierarchy for a field with the the given name and makes sure it is settable via Field.setAccessible().
     *
     * @param fieldName the field to find
     * @param clazz     the class to find it in
     * @return the first Field found with name
     */
    public static Field getField(String fieldName, Class clazz) {
        if (fieldName == null || clazz == null) {
            return null;
        }

        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) {
                field.setAccessible(true);
                return field;
            }
        }

        if (clazz.getSuperclass() != null && !clazz.equals(clazz.getSuperclass())) {
            return getField(fieldName, clazz.getSuperclass());
        }

        return null;
    }

    /**
     * Gets all the fields from from all classes in the inheritance hierarchy EXCEPT for any class who's packages starts with "java*".
     *
     * @param clazz the class to search
     * @return all Fields in the inheritance hierarchy other "java*" packages classes.
     */
    public static List<Field> getFields(Class clazz) {
        Set<String> found  = new HashSet<>();
        List<Field> fields = new ArrayList<>();

        do {
            if (clazz.getName().startsWith("java"))
                break;

            Field[] farr = clazz.getDeclaredFields();
            for (Field f : farr) {
                if (!found.contains(f.getName())) {
                    try {
                        f.setAccessible(true);
                        found.add(f.getName());
                        fields.add(f);
                    } catch (Exception ex) {
                        //ignore inaccessable fields
                    }

                } else {
                    //System.out.println("This super class property is being skipped because it is being hidden by a child class property with the same name...is this a design mistake? " + f);
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != null && !Object.class.equals(clazz));

        return fields;
    }

    /**
     * Searches the inheritance hierarchy for the first method of the given name (ignores case).
     * <p>
     * No distinction is made for overloaded method names.
     *
     * @param clazz the class to search
     * @param name  the name of a method to find
     * @return the first method with name
     */
    public static Method getMethod(Class clazz, String name) {
        //System.out.println("looking for " + name + " ---------------------------------");
        while (clazz != null && !Object.class.equals(clazz)) {
            for (Method m : clazz.getMethods()) {
                if (m.getName().equalsIgnoreCase(name)){
                    //System.out.println("FOUND: " + m.getName());
                    return m;
                }
                else {
                    //System.out.println(name + " != " + m.getName());
                }
            }
            clazz = clazz.getSuperclass();
        }

        return null;
    }

    /**
     * Tries to find a bean property getter then defaults to returning the Field value
     *
     * @param name   the bean property value to find
     * @param object the object to find it in
     * @return the value of the bean property with name
     */
    public static Object getProperty(String name, Object object) {
        try {
            Method getter = getMethod(object.getClass(), "get" + name);
            if (getter != null) {
                return getter.invoke(object);
            } else {
                Field field = getField(name, object.getClass());
                if (field != null)
                    return field.get(object);
            }
        } catch (Exception ex) {
            //ex.printStackTrace();
        }
        return null;
    }

    /**
     * Finds an input stream for <code>fileOrUrl</code> and reads it into a string
     *
     * @param fileOrUrl the resource to read
     * @return the content of <code>code</code> as a String
     * @see #findInputStream(String)
     */
    public static String read(String fileOrUrl) {
        return read(findInputStream(fileOrUrl));
    }

    /**
     * Read all of the stream to a string and close the stream.
     *
     * @param in the data to stringify
     * @return the data from in as a string
     * @throws RuntimeException when an IOException is thrown
     */
    public static String read(InputStream in) throws RuntimeException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pipe(in, out);
            return new String(out.toByteArray());
        } catch (Exception ex) {
            rethrow(ex);
        }
        return null;
    }

    /**
     * Read the contents of a file to a string
     *
     * @param file the file to read and stringify
     * @return the file text
     * @throws IOException when an IOException is thrown
     * @see #read(InputStream)
     */
    public static String read(File file) throws IOException {
        return read(new BufferedInputStream(new FileInputStream(file)));
    }

    /**
     * Write the string value to a file
     *
     * @param file the file to write
     * @param text the content to write
     * @throws IOException if unable to create the parent directory or when IO fails
     */
    public static void write(File file, String text) throws IOException {
        File dir = file.getParentFile();
        if (!dir.exists())
            if (!dir.mkdirs())
                throw new IOException("Unable to create the parent directory");

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
        bw.write(text);
        bw.flush();
        bw.close();
    }

    /**
     * Convenience overloading of write(File, String)
     *
     * @param file the file path to write
     * @param text the text to write to the file
     * @throws IOException when thrown
     * @see #write(File, String)
     */
    public static void write(String file, String text) throws IOException {
        if (text == null)
            return;
        write(new File(file), text);
    }

    /**
     * Copy all data from src to dst and close the streams
     *
     * @param in  the data to be written
     * @param out where the data should be written to
     * @throws IOException when thrown
     */
    public static void pipe(InputStream in, OutputStream out) throws IOException {
        int    read;
        byte[] buffer = new byte[1024];
        while ((read = in.read(buffer)) > -1) {
            out.write(buffer, 0, read);   // Don't allow any extra bytes to creep in, final write
        }
        out.close();
    }


    public static File createTempFile(String fileName) throws IOException {
        if (empty(fileName))
            fileName = "working.tmp";

        fileName = fileName.trim();
        fileName = fileName.replace('\\', '/');

        if (fileName.endsWith("/")) {
            fileName = "working.tmp";
        } else {
            if (fileName.lastIndexOf('/') > 0) {
                fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
            }
        }

        if (empty(fileName))
            fileName = "working.tmp";

        fileName = slugify(fileName);
        if (fileName.lastIndexOf('.') > 0) {
            String prefix = fileName.substring(0, fileName.lastIndexOf('.'));
            String suffix = fileName.substring(fileName.lastIndexOf('.'));

            return File.createTempFile(prefix + "-", suffix);
        } else {
            return File.createTempFile(fileName, "");
        }
    }

    /**
     * Attempts to locate the stream as a file, url, or classpath resource
     *
     * @param fileOrUrl a stream resource identifier
     * @return an input stream reading fileOrUrl
     * @throws RuntimeException when and IOException is thrown
     */
    public static InputStream findInputStream(String fileOrUrl) throws RuntimeException {
        try {
            if (fileOrUrl.startsWith("file:/")) {
                fileOrUrl = URLDecoder.decode(fileOrUrl, "UTF-8");
            }
            if (fileOrUrl.startsWith("file:///")) {
                fileOrUrl = fileOrUrl.substring(7);
            }
            if (fileOrUrl.startsWith("file:/")) {
                fileOrUrl = fileOrUrl.substring(5);
            }

            if (fileOrUrl.indexOf(':') >= 0) {
                return new URL(fileOrUrl).openStream();
            } else if (new File(fileOrUrl).exists()) {
                return new FileInputStream(fileOrUrl);
            } else {
                return Thread.currentThread().getContextClassLoader().getResourceAsStream(fileOrUrl);
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Checks <code>string</code> for wildcard control characters.
     * @param string
     * @return true when <code>string</code> contains a regex control character
     */
    public static boolean isRegex(String string){
        String controlChars = "<([{\\^-=$!|]})?*+.>";
        for(int i=0; i<controlChars.length(); i++){
            if(string.indexOf(controlChars.charAt(i)) > 0)
                return true;
        }
        return false;
    }

    /**
     * @param str the string to chec to see if it is a wildcard pattern.
     * @return true if the string contains a * or a ?
     */
    public static boolean isWildcard(String str) {
        return str.indexOf('*') >= 0 || str.indexOf('?') >= 0;
    }

    /**
     * Pattern matches the string using ? to indicate any one single value and * to indicate any 0-n multiple value
     *
     * @param wildcard a wildcard pattern
     * @param string   the string to check to see if it matches the wildcard
     * @return true if string matches wildcard
     */
    public static boolean wildcardMatch(String wildcard, String string) {
        if (wildcard.equals("*"))
            return true;

        if (empty(wildcard) || empty(string))
            return false;

        if (!isWildcard(wildcard))
            return wildcard.equals(string);
        else
            return string.matches(wildcardToRegex(wildcard));
    }

    /**
     * Converts a * and ? wildcard style patterns into regex style pattern
     *
     * @param wildcard the wildcard expression to convert to a regex
     * @return a wildcard pattern converted to a regex
     * @see <a href="http://www.rgagnon.com/javadetails/java-0515.html">Code example found here</a>
     */
    public static String wildcardToRegex(String wildcard) {
        wildcard = wildcard.replace("**", "*");
        StringBuilder s = new StringBuilder(wildcard.length());
        s.append('^');
        for (int i = 0, is = wildcard.length(); i < is; i++) {
            char c = wildcard.charAt(i);
            switch (c) {
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
                default:
                    s.append(c);
                    break;
            }
        }
        s.append('$');
        return (s.toString());
    }

    public static LinkedHashMap<String, String> parseQueryString(String query) {

        //TODO: add support for multiple values by concatinating with ","
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        if (query != null) {
            try {
                while (query.startsWith("?") || query.startsWith("&") || query.startsWith("=")) {
                    query = query.substring(1);
                }

                if (query.length() > 0) {
                    String[] pairs = query.split("&");
                    for (String pair : pairs) {
                        pair = pair.trim();

                        if (pair.length() == 0)
                            continue;

                        int idx = pair.indexOf("=");
                        if (idx > 0) {
                            String key = pair.substring(0, idx).trim();
                            key = URLDecoder.decode(key, "UTF-8");

                            String value = pair.substring(idx + 1).trim();
                            value = URLDecoder.decode(value, "UTF-8");

                            params.put(key, value);
                        } else {
                            params.put(URLDecoder.decode(pair, "UTF-8"), null);
                        }
                    }
                }
            } catch (Exception ex) {
                rethrow(ex);
            }
        }
        return params;
    }

    public static String findSysEnvProp(String... names) {
        String value;
        for (String name : names) {
            value = getSysEnvProp(name);
            if (value != null)
                return value;
        }
        return null;
    }

    public static String getSysEnvProp(String name) {
        String value = System.getProperty(name);

        if (Utils.empty(value))
            // try replacing dot for underscores, since Lambda doesn't support dots in env vars
            value = System.getProperty(name.replace(".", "_"));

        if (Utils.empty(value))
            value = System.getenv(name);

        if (Utils.empty(value))
            // try replacing dot for underscores, since Lambda doesn't support dots in env vars
            value = System.getenv(name.replace(".", "_"));

        if (Utils.empty(value)) {
            InputStream stream = findInputStream(".env");
            if (stream != null) {
                Properties p = new Properties();
                try {
                    p.load(stream);
                    Utils.close(stream);
                    value = p.getProperty(name);
                } catch (Exception ex) {
                    Utils.rethrow(ex);
                }
            }
        }

        return value;
    }

    public static Object castDbOutput(String type, Object value) {

        if (type == null || value == null)
            return value;

        type = type.toLowerCase();

        if ("json".equalsIgnoreCase(type)) {
            String json = value.toString().trim();
            if (json.isEmpty())
                return new JSNode();
            return JSNode.parseJson(json);
        }

        if (Utils.in(type, "char", "nchar", "clob"))
            value = value.toString().trim();

        if (value instanceof Date && Utils.in(type, "date", "datetime", "timestamp")) {
            value = formatIso8601((Date) value);
        }

        return value;
    }

    public static Object castJsonInput(String type, Object value) {
        try {
            if (value == null)
                return null;

            if (type == null) {
                try {
                    if (!value.toString().contains(".")) {
                        return Long.parseLong(value.toString());
                    } else {
                        return Double.parseDouble(value.toString());
                    }
                } catch (Exception ex) {
                    //must not have been an number
                }
                return value.toString();
            }

            switch (type.toLowerCase()) {
                case "char":
                case "nchar":
                case "clob":
                    return value.toString().trim();
                case "s":
                case "string":
                case "varchar":
                case "nvarchar":
                case "longvarchar":
                case "longnvarchar":
                case "json":
                    return value.toString();
                case "n":
                case "number":
                case "numeric":
                case "decimal":
                    if (!value.toString().contains("."))
                        return Long.parseLong(value.toString());
                    else
                        return Double.parseDouble(value.toString());

                case "bool":
                case "boolean":
                case "bit": {
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
                    throw new UnsupportedOperationException("Binary types are currently unsupported");

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
                    else {
                        String json = value.toString().trim();
                        if (json.length() > 0) {
                            char c = json.charAt(0);
                            if (c == '[' || c == '{')
                                return JSNode.parseJson(value + "");
                        }
                        return json;
                    }

                default:
                    throw ApiException.new500InternalServerError("Error casting '{}' as type '{}'", value, type);
            }
        } catch (Exception ex) {
            Utils.rethrow(ex);
            //throw new RuntimeException("Error casting '" + value + "' as type '" + type + "'", ex);
        }

        return null;
    }

    /**
     * Utility to call a close() method on supplied objects if it exists and completely ignore any exceptions.
     *
     * @param toClose the object to close.
     */
    public static void close(Object... toClose) {
        for (Object o : toClose) {
            if (o != null) {
                try {
                    if (o instanceof Closeable) {
                        ((Closeable) o).close();
                    } else {
                        Method m = o.getClass().getMethod("close");
                        m.invoke(o);
                    }
                    //} catch (NoSuchMethodException ex) {
                    //ignore
                } catch (Exception ex) {
                    //ignore
                }
            }
        }
    }

}