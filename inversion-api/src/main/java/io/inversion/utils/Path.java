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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import io.inversion.ApiException;

/**
 * A case insensitive utility abstraction for working with forward slash based paths /like/you/find/in/urls.
 * <p>
 * When working with Paths, leading and trailing slashes are completely disregarded. 
 * Any leading and trailing slashes in the documentation are illustrative only and could be excluded to achieve the same result.  
 * Multiple consecutive slashes are treated as a single slash.  
 * There will never be an empty string or null path part.
 * <p>
 * In addition to representing concrete paths, such as a file path or url path, a Path object may 
 * contain wildcards, regular expressions, and variable name bindings which are used when comparing a variablized 
 * abstract paths with a concrete path.
 * <p>
 * Paths are primarily used to configure Rules (Engine, Api, Endpoint and Action are all subclasses of Rule)
 * to match against inbound Request urls to determine how the Request will be processed.
 * <p>
 * Paths can be variablized as follows:
 * <ul>
 *  <li>/animals/* - will match anything that starts with "animals/".  
 *      "*" matches any number of path segments including zero segments (meaning "animals/" alone will match and so will "animals/dogs/fido").
 *      "*" wildcards are only valid as the last segment in a path.
 *  
 *  <li>/animals/dogs/:dogName - if a path segment starts with a ":" it indicates that the value can be anything but a value is required and should be 
 *      mapped to the corresponding variable name, in this case "dogName", by whoever is doing the path matching.
 *  
 *  <li>/animals/dogs/{fido|ralph|jackie} - if the path segment is wrapped in "{}" the contents are considered a regular expression {@link java.util.regex.Pattern} for matching
 *  
 *  <li>/animals/dogs/${fido|ralph|jackie} - a '$' can prefix '{}' as syntatic sugar some may be familiar with.  
 *      
 *  <li>/animals/dogs/{dogName:fido|ralph|jackie} - again a regex, this time with a variable binding to "dogName"
 *  
 *  <li>/animals/[{dogs|cats|snakes}]/:animalName/* - if something is wrapped in square brackets "[]" that segment and all subsequent segments
 *      are optional.  If the segments exists in the path being compared to, they must match the supplied rules, but if the 
 *      comparison path ends right before the first optional segment, the paths still match.
 *
 *  </ul>
 * <p>
 *  
 * When used in the context of a Api configuration you may see something like this:
 * <p>
 * <pre>
 *  Engine e = new Engine().withIncludeOn(null, new Path("/apis"));
 *                         .withApi(new Api().withIncludeOn(null, new Path("bookstore/{storeName:johnsBestBooks|carolsBooksOnMain}"))
 *                                           .withEndpoint(new Endpoint().withIncludeOn(null, new Path("categories/:category/"))
 *                                                                       .withAction(new BrowseCategoriesAction().withIncludeOn(null, new Path("[:subcategory]/*"))))); 
 * </pre>
 * 
 * @see io.inversion.Rule.withIncludeOn
 * @see io.inversion.Rule.withExcludeOn
 * 
 */

public class Path {

   List<String> parts = new ArrayList();
   List<String> lc    = new ArrayList();

   /**
    * Creates an empty Path
    */
   public Path() {

   }

   /**
    * Creates a clone of the supplied <code>Path</code>
    * @param path  the Path to be cloned
    */
   public Path(Path path) {
      parts.addAll(path.parts);
      lc.addAll(path.lc);
   }

   /**
    * Constructs a Path based on all of the supplied <code>part</code>s.
    * <p>
    * The strings in <code>part</code> may themselves contain "/" characters and will be split into multiple parts correspondingly.  
    * Meaning <code>Path p = new Path("part1", "part2/part3", "/part4/", "////part5//part6/", "part7")</code> is valid
    * and would result in a Path with parts "part1", "part2", "part3", "part4", "part5", "part6", "part7".  
    * 
    * @param path  an array of path part strings
    */
   public Path(String... part) {
      parts = Utils.explode("/", part);
      lc = new ArrayList(parts.size());
      for (int i = 0; i < parts.size(); i++) {
         lc.add(parts.get(i).toLowerCase());
      }
   }

   /**
    * Convenience overload of {@link #Path(String...)}.
    * 
    * @param parts  an list of path part strings
    */
   public Path(List<String> parts) {
      this(parts.toArray(new String[parts.size()]));
   }

   /**
    * Gets the path parts as a List.
    * <p>
    * Method signature could easily have been "asList()"
    * 
    * @return a new list with the individual path parts n the originally defined case.
    */
   public List<String> parts() {
      return new ArrayList(parts);
   }

   /**
    * Simple way to pull the first element of the path without having to check for <code>size() > 0</code> first.
    * 
    * @return the first element in the path if it exists otherwise null 
    */
   public String first() {
      if (parts.size() > 0)
         return parts.get(0);
      return null;
   }

   /**
    * Simple way to pull the last element of the path without having to check for <code>size() > 0</code> first.
    * 
    * @return the last element in the path if it exists otherwise null
    */
   public String last() {
      if (parts.size() > 0)
         return parts.get(parts.size() - 1);
      return null;
   }

   /**
    * Simple way to get element at <code>index</code> without haveint to check for <code>size() > index</code> first.
    * 
    * @param index the index of the path part to retrive
    * @return the path part at index if it exists otherwise null
    */
   public String get(int index) {
      if (index < parts.size())
         return parts.get(index);
      return null;
   }

   /**
    * Adds <code>part</code> to the end of the Path.
    * <p>
    * The <code>parts</code> is exploded via <code>Utils.explode('/', part)</code> first so while the part arg is a 
    * single value, it could result in multiple additions. 
    * 
    * @param part
    */
   public void add(String parts) {
      if (!Utils.empty(parts)) {
         for (String part : Utils.explode("/", parts)) {
            this.parts.add(part);
            lc.add(part.toLowerCase());
         }
      }
   }

   /**
    * Simple way to remove the path part at <code>index</code> without having to check for <code>size() < index</code> first.
    * 
    * @param index  the index of the path part to remove
    * @return the path part previously located at <code>index</code> if it existed otherwise null
    */
   public String remove(int index) {
      if (index < parts.size()) {
         lc.remove(index);
         return parts.remove(index);
      }
      return null;
   }

   /**
    * Performs a case insensitive string match between this Path and <code>pathsToMatch</code>.  
    * <p>
    * Wildcards and regular expressions are not supported in this method, only straight case insensitive string comparison.
    *  
    * @param partsToMatch 
    * @return true if each index of <code>partsToMatch</code> is a case insensitive match to this Path at the same index otherwise false.
    */
   public boolean startsWith(List<String> partsToMatch) {
      if (partsToMatch.size() > this.parts.size())
         return false;

      for (int i = 0; i < partsToMatch.size(); i++) {
         if (!partsToMatch.get(i).equalsIgnoreCase(this.parts.get(i)))
            return false;
      }
      return true;
   }

   /**
    * @return the number of parts in the Path
    */
   public int size() {
      return parts.size();
   }

   /**
    * @return a pretty printed "/" separated path string representation 
    */
   public String toString() {
      return Utils.implode("/", parts);
   }

   /**
    * @return true of the objects string representations match
    */
   public boolean equals(Object o) {
      if (o == null)
         return false;

      return o.toString().equals(toString());
   }

   /**
    * Creates a new sub Path.
    *
    * @param fromIndex low endpoint (inclusive) of the subList
    * @param toIndex high endpoint (exclusive) of the subList
    * @return a subpath from <code>fromIndex</code> (inclusive) to <code>toIndex<code> (exclusive)
    * @throws IndexOutOfBoundsException 
    */
   public Path subpath(int fromIndex, int toIndex) throws ArrayIndexOutOfBoundsException {
      Path subpath = new Path(parts.subList(fromIndex, toIndex));
      return subpath;
   }

   /**
    * Checks to see if the value at <code>index</code> is a wildcard, a variable, or is optional.
    * 
    * @param index the path part to check
    * @return true if the path part at <code>index</code> is a '*' char or starts with '[', '{' or ':' 
    */
   public boolean isStatic(int index) {
      return !(isWildcard(index) || isVar(index) || isOptional(index));
   }

   /**
    * Check if the path part at <code>index</code> is equal to '*' without having
    * to check if <code>size() < index</code> first.
    * 
    * @param index
    * @return true if the path part at <code>index</code> 
    */
   public boolean isWildcard(int index) {
      return "*".equals(get(index));
   }

   /**
    * Check to see if the value at <code>index</code> starts with '${', '{', ':' after removing any leading '[' characters.
    * <p>
    * @param index
    * @return true if the value exists and is variableized but not a wildcard, false otherwise.
    */
   public boolean isVar(int index) {
      String part = get(index);
      if (part != null) {
         if (part.startsWith("["))
            part = part.substring(1).trim();

         char c = part.charAt(0);
         return c == '$' || c == ':' || c == '{';
      }
      return false;
   }

   /**
    * Extracts a variable name form the path expression if <code>index</code> exists and has a var name.
    * <p>
    * 'varName' would be extracted from <code>getVarName(1)</code> for the following paths.
    * <ul>
    *  <li> /part/:varName/
    *  <li> /part/{varNam:regex}/
    *  <li> /part/${varNam:regex}/
    * </ul>
    * 
    * Square brackets indicating a path component is optioanl don't impact retrieval of the var name so the following
    * would return the same as there above counterparts:
    * <ul>
    *  <li> /part/[:varName]/ 
    *  <li> /[part]/{varNam:regex}]/
    *  <li> /[part]/[${varNam:regex}]/
    * </ul>
    *  
    * @param  index
    * @return the variable name binding for the parth part at <code>index</code> if it exists
    */
   public String getVarName(int index) {
      String part = get(index);
      if (part != null) {
         if (part.startsWith("["))
            part = part.substring(1, part.length() - 1).trim();

         int colon = part.indexOf(":");
         if (colon == 0)
            return part.substring(1).trim();
         else if (part.startsWith("{") && colon > 1)
            return part.substring(1, colon).trim();
      }
      return null;
   }

   /**
    * Square brackets, '[]', indicate that a path path (and by implication, all following parts) are considered optional for path matching.
    *  
    * For example: <code>new Path("first/second/[third]/").matches(new Path("first/second/third"))</code> 
    *  
    * @param index
    * @return true if the path part at <code>index</code> exists and starts with '[' and ends with ']'
    */
   public boolean isOptional(int index) {
      String part = get(index);
      if (part != null) {
         return part.startsWith("[") && part.endsWith("]");
      }
      return false;
   }

   /**
    * Convenience overloading of {@link #matches(Path)}.
    * 
    * @param toMatch
    * @return true if the paths match
    */
   public boolean matches(String toMatch) {
      return matches(new Path(toMatch));
   }

   /**
    * Checks if this Path is as case insensitive match, including any optional rules, wildcards, and regexes to <code>concretePath</path>.
    * <p>
    * As the name implies <code>concretePath</code> is considered to be a literal path not containing optionals, wildcards, and regexes. 
    * <p>
    * As also documented above:
    * <ul>
    *   <li>paths can end with a "/*" character meaning this and all following parts are optional and unconstrained
    *   <li>wrapping a path part in square brackets, '[]' indicates that it and all following part parts are optional.
    *   <li>wrapping a path part in '${}' or '${}' indicates that this path part should match via a regular expression such as '${[0-9a-fA-F]{1,8}}' to match a 1 to 8 character alpha numeric part
    *   <li>you can bind a variable name to a regex by preceding the regex with a name and a colon '/${id:[0-9a-fA-F]{1,8}}/'
    *   <li>starting a path part with a ':' such as '/:id/' is functionally equivalent to '/${id:.*}'
    * </ul>
    * <p>
    * All non regex comparisons are performed with String.equalsIgnoreCase.
    * <p>
    * All regexes are compiled with Pattern.CASE_INSENSITIVE.
    * 
    * @param concretePath
    * @return true if this path matches <code>concretePath</code>
    */
   public boolean matches(Path concretePath) {
      Path matchedPath = new Path();

      if (size() < concretePath.size() && !"*".equals(last())) {
         return false;
      }

      for (int i = 0; i < size(); i++) {
         String myPart = get(i);

         if (i == size() - 1 && myPart.equals("*"))
            return true;

         boolean optional = myPart.startsWith("[") && myPart.endsWith("]");

         if (i == concretePath.size()) {
            if (optional)
               return true;
            return false;
         }

         if (optional)
            myPart = myPart.substring(1, myPart.length() - 1);

         String theirPart = concretePath.get(i);
         matchedPath.add(theirPart);

         if (myPart.startsWith(":")) {
            continue;
         }
         else if ((myPart.startsWith("{") || myPart.startsWith("${")) && myPart.endsWith("}")) {
            int nameStart = myPart.indexOf("{") + 1;
            int endName = myPart.indexOf(":");
            if (endName < 0)
               endName = myPart.length() - 1;

            String name = myPart.substring(nameStart, endName).trim();

            if (endName < myPart.length() - 1) {
               String regex = myPart.substring(endName + 1, myPart.length() - 1);
               Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
               if (!pattern.matcher(theirPart).matches()) {
                  return false;
               }
            }
         }
         else if (!myPart.equalsIgnoreCase(theirPart)) {
            return false;
         }

      }

      return true;
   }

   /**
    * Convenience overloading of {@link #extract(Map, Path, boolean)} with <code>greedy = true</code>.
    * 
    * @see Path#extract(Map, Path, boolean)
    */
   public Path extract(Map params, Path toMatch) {
      return extract(params, toMatch, false);
   }

   /**
    * Consumes the matching parts of <code>matchingConcretePath</code> and extracts any named variable to <code>matchingConcretePath</code> if this 
    * any of this paths parts contain variable bindings.
    * <p>
    * If <code>greedy</code> is true, the match will consume through matching optional path parts.  
    * <p>
    * If <code>greedy</code> is false, variable bindings in any optional paths parts will be extracted but the parts will not be removed from <code>matchingConcretePath</code>.
    * <p>
    * Here is an example:
    * <pre>
    *   Map params = new HashMap();
    *   Path engineMatch = new Path("apis/myapi/*");
    *   Path apiMatch = new Path("${version:v1|v2}/:tenant")
    *   Path endpointMatch = new Path("[${collection:books|categories|orders}]/*");
    *   Path actionMatch = new Path("[:orderId]/*");
    * 
    *   Path requestPath = new Path("/apis/myapi/v2/bobsBooks/orders/67890");
    *   
    *   engineMatch.extract(requestPath);
    *   // params is empty, requestPath is now 'v2/bobsBooks/orders/67890'
    *   
    *   apiMatch.extract(requestPath);
    *   //version=v2 and tenant=bobsBooks have been added to params and requestPath is now 'orders/67890'
    *   
    *   endpointMatch.extract(requestPath);
    *   //collection=orders has been added to params and requestPath is now '67890'
    *   
    *   actionMatch.extract(requestPath);
    *   //orderId=67890 has been added to params and requestPath is now empty.
    * </pre>
    * <p>
    * The above example is very similar to how an {@link io.inversion.Engine} processes paths when selecting an Api, Endpoint, and Actions to run.
    * <p> 
    * Engine will also add the params to the Request Url params as if they had been supplied as name value pairs by the caller on the query string.
    * 
    * @param params
    * @param matchingConcretePath
    * @param greedy
    * @return the same Path object that was passed in but potentially now shorter as matching segments may have been consumed
    * @see {@code io.inversion.Engine#service(io.inversion.Request, io.inversion.Response)}
    * @see {@code io.inversion.Chain#next}
    * 
    */
   public Path extract(Map params, Path matchingConcretePath, boolean greedy) {
      Path matchedPath = new Path();

      boolean restOptional = false;
      int i = 0;
      int nextOptional = 0;
      for (i = 0; i < size() && matchingConcretePath.size() > 0; i++) {
         String myPart = get(i);

         boolean partOptional = myPart.startsWith("[") && myPart.endsWith("]");

         if (partOptional) {
            restOptional = true;
            myPart = myPart.substring(1, myPart.length() - 1);
         }

         if (myPart.equals("*"))
            break;

         String theirPart = null;

         if (greedy || !restOptional) {
            theirPart = matchingConcretePath.remove(0);
            matchedPath.add(theirPart);
         }
         else {
            theirPart = matchingConcretePath.get(nextOptional++);
         }

         if (myPart.startsWith(":")) {
            String name = myPart.substring(1).trim();
            params.put(name, theirPart);
         }
         else if ((myPart.startsWith("{") || myPart.startsWith("${")) && myPart.endsWith("}")) {
            int nameStart = myPart.indexOf("{") + 1;
            int endName = myPart.indexOf(":");
            if (endName < 0)
               endName = myPart.length() - 1;

            String name = myPart.substring(nameStart, endName).trim();

            if (endName < myPart.length() - 1) {
               String regex = myPart.substring(endName + 1, myPart.length() - 1);
               Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
               if (!pattern.matcher(theirPart).matches()) {
                  ApiException.throw500InternalServerError("Attempting to extract values from an unmatched path: '{}', '{}'", this.parts.toString(), matchingConcretePath.toString());
               }
            }

            params.put(name, theirPart);
         }
         else if (!myPart.equalsIgnoreCase(theirPart)) {
            ApiException.throw500InternalServerError("Attempting to extract values from an unmatched path: '{}', '{}'", this.parts.toString(), matchingConcretePath.toString());
         }
      }

      //null out any trailing vars
      for (i = i; i < size(); i++) {
         String var = getVarName(i);
         if (var != null)
            params.put(var, null);
      }

      return matchedPath;
   }
}
