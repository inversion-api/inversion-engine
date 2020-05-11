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
package io.inversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inversion.rql.Term;
import io.inversion.utils.Path;
import io.inversion.utils.Utils;
import io.inversion.utils.Rows.Row;

public abstract class Db<T extends Db>
{
   protected final Logger          log           = LoggerFactory.getLogger(getClass());

   transient volatile boolean      started       = false;
   transient volatile boolean      starting      = false;
   transient volatile boolean      shutdown      = false;

   /**
    * A CSV of pipe delimited table name to collection pairs
    * 
    * Example: db.tables=promo-dev|promo,loyalty-punchcard-dev|punchcard
    * 
    * Or if the collection name is the name as the table name you can just send a the name
    * 
    * Example: db.includeTables=orders,users,events
    */
   protected Map<String, String>   includeTables = new HashMap();

   protected boolean               bootstrap     = true;

   protected String                name          = null;
   protected String                type          = null;

   protected Path                  endpointPath  = null;

   protected ArrayList<Collection> collections   = new ArrayList();

   public Db()
   {
   }

   public Db(String name)
   {
      this.name = name;
   }

   public synchronized Db startup(Api api)
   {
      if (started || starting) //starting is an accidental recursion guard
         return this;

      starting = true;
      try
      {
         doStartup(api);

         started = true;
         return this;
      }
      finally
      {
         starting = false;
      }
   }

   /**
    * Made to be overridden by subclasses 
    * or anonymous inner classes to do specific init
    */
   protected void doStartup(Api api)
   {
      try
      {
         if (isBootstrap())// && getTables().size() == 0)
         {
            configDb();
            configApi(api);
         }
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
         Utils.rethrow(ex);
      }
   }

   public synchronized void shutdown()
   {
      if ((started || starting) && !shutdown)
      {
         try
         {
            doShutdown();
         }
         finally
         {
            shutdown = true;
         }
      }
   }

   /**
    * Made to be overridden by subclasses 
    * or anonymous inner classes to do specific init
    */
   protected void doShutdown()
   {

   }

   /**
    * Finds all rows that match the supplied query terms.  
    * 
    * IMPORTANT The Result object contains a list of Row objects. Unlike the Rows class
    * all Row objects in the Result do not have to share the same keys. 
    * 
    * @param table
    * @param queryTerms
    * @return
    * @throws Exception
    */
   public abstract Results select(Collection collection, List<Term> queryTerms) throws Exception;

   /**
    * Upserts the key/values pairs for each row into the underlying data source as a PATCH,
    * not as a full replacement.  Keys that are not supplied in the call but that exist in the row in 
    * the target DB should not be modified.
    * 
    * Each row should minimally contain key value pairs that satisfy one of the 
    * tables unique index constraints allowing an update to take place instead  
    * of an insert if the row already exists in the underlying data source.
    *
    * IMPORTANT #1 - implementors should note that the keys on each row may be different.
    * 
    * IMPORTANT #2 - strict POST/PUT vs POST/PATCH semantics are implementation specific.
    * For example, a RDBMS backed implementation may choose to upsert only the supplied
    * client supplied keys effectively making this a POST/PATCH operation.  A 
    * document store that is simply storing the supplied JSON may not be able to do
    * partial updates elegantly and replace existing documents entirely rendering
    * this a POST/PUT.    
    * 
    * @param table
    * @param rows
    * @return
    * @throws Exception
    */
   public abstract List<String> upsert(Collection collection, List<Map<String, Object>> rows) throws Exception;

   public List<Integer> patch(Collection collection, List<Map<String, Object>> rows) throws Exception
   {
      upsert(collection, rows);
      List counts = new ArrayList();
      rows.forEach(row -> counts.add(-1));
      return counts;
   }

   /**
    * Deletes rows identified by the unique index values from the underlying data source.
    * 
    * IMPORTANT implementors should note that the keys on each row may be different.
    * The keys should have come from a unique index, meaning that the key/value pairs
    * for each row should uniquely identify the row...however there is no guarantee 
    * that each row will reference the same index.
    * 
    * @param table
    * @param indexValues
    * @throws Exception
    */
   public abstract void delete(Collection collection, List<Map<String, Object>> indexValues) throws Exception;

   public void configDb() throws Exception
   {
      for (String key : includeTables.keySet())
      {
         withCollection(new Collection(key));
      }
   }

   public void configApi(Api api)
   {
      List<String> relationshipStrs = new ArrayList();

      for (Collection coll : getCollections())
      {
         //if (!coll.isLinkTbl() && !coll.isExclude())
         if (!coll.isExclude())
         {
            api.withCollection(coll);
         }
      }

      for (Collection coll : getCollections())
      {
         //         if (coll.isLinkTbl())
         //            continue;

         if (coll.getName().equals(coll.getTableName()))
         {
            //collection has not already been specifically customized
            String prettyName = beautifyCollectionName(coll.getTableName());
            coll.withName(prettyName);
         }

         for (Property prop : coll.getProperties())
         {
            if (prop.getColumnName().equals(prop.getJsonName()))
            {
               //json name has not already been specifically customized
               String prettyName = beautifyAttributeName(prop.getColumnName());
               prop.withJsonName(prettyName);
            }

         }
      }

      //-- Now go back through and create relationships for all foreign keys
      //-- two relationships objects are created for every relationship type
      //-- representing both sides of the relationship...MANY_TO_ONE also
      //-- creates a ONE_TO_MANY and there are always two for a MANY_TO_MANY.
      //-- API designers may want to represent one or both directions of the
      //-- relationship in their API and/or the names of the JSON properties
      //-- for the relationships will probably be different
      for (Collection coll : getCollections())
      {
         if (coll.isLinkTbl())
         {
            //create reciprocal pairs for of MANY_TO_MANY relationships
            //for each pair combination in the link table.
            List<Index> indexes = coll.getIndexes();
            for (int i = 0; i < indexes.size(); i++)
            {
               for (int j = 0; j < indexes.size(); j++)
               {
                  Index idx1 = indexes.get(i);
                  Index idx2 = indexes.get(j);

                  if (i == j || !idx1.getType().equals("FOREIGN_KEY") || !idx2.getType().equals("FOREIGN_KEY"))
                     continue;

                  Collection resource1 = idx1.getProperty(0).getPk().getCollection();
                  Collection resource2 = idx2.getProperty(0).getPk().getCollection();

                  Relationship r = new Relationship();
                  r.withType(Relationship.REL_MANY_TO_MANY);

                  r.withRelated(resource2);
                  r.withFkIndex1(idx1);
                  r.withFkIndex2(idx2);
                  r.withName(makeRelationshipName(resource1, r));
                  r.withCollection(resource1);
                  relationshipStrs.add(r.toString());
               }
            }
         }
         else
         {
            for (Index fkIdx : coll.getIndexes())
            {
               try
               {
                  if (!fkIdx.getType().equals("FOREIGN_KEY") || fkIdx.getProperty(0).getPk() == null)
                     continue;

                  Collection pkResource = fkIdx.getProperty(0).getPk().getCollection();
                  Collection fkResource = fkIdx.getProperty(0).getCollection();

                  //ONE_TO_MANY
                  {
                     Relationship r = new Relationship();
                     //TODO:this name may not be specific enough or certain types
                     //of relationships. For example where an resource is related
                     //to another resource twice
                     r.withType(Relationship.REL_ONE_TO_MANY);
                     r.withFkIndex1(fkIdx);
                     r.withRelated(fkResource);
                     r.withName(makeRelationshipName(pkResource, r));
                     r.withCollection(pkResource);
                     relationshipStrs.add(r.toString());
                  }

                  //MANY_TO_ONE
                  {
                     Relationship r = new Relationship();
                     r.withType(Relationship.REL_MANY_TO_ONE);
                     r.withFkIndex1(fkIdx);
                     r.withRelated(pkResource);
                     r.withName(makeRelationshipName(fkResource, r));
                     r.withCollection(fkResource);
                     relationshipStrs.add(r.toString());
                  }
               }
               catch (Exception ex)
               {
                  ApiException.throw500InternalServerError(ex, "Error creating relationship for index: {}", fkIdx);
               }
            }
         }
      }

      //TODO...should this operate on all tables or just this DBs tables...?
      //now we need to see if any relationship names conflict and need to be made unique
      for (Collection coll : api.getCollections())
      {
         List<Relationship> relationships = coll.getRelationships();

         for (int i = 0; i < relationships.size(); i++)
         {
            String nameA = relationships.get(i).getName();

            for (int j = i + 1; j < relationships.size(); j++)
            {
               String nameB = relationships.get(j).getName();

               if (nameA.equalsIgnoreCase(nameB))
               {
                  String uniqueName = makeRelationshipUniqueName(coll, relationships.get(j));
                  relationships.get(j).withName(uniqueName);
               }
            }
         }
      }
   }

   protected String beautifyCollectionName(String name)
   {
      if (includeTables.containsKey(name))
         return includeTables.get(name);

      name = beautifyAttributeName(name);

      if (!(name.endsWith("s") || name.endsWith("S")))
         name = Pluralizer.plural(name);

      return name;
   }

   /**
    * Try to make a camel case valid java script variable name.
    * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Grammar_and_types#Variables
    * 
    * @param name
    * @return
    */
   protected String beautifyAttributeName(String name)
   {
      //all upper case...U.G.L.Y you ain't got on alibi you UGLY, hay hay you UGLY
      if (name.toUpperCase().equals(name))
      {
         name = name.toLowerCase();
      }

      StringBuffer buff = new StringBuffer("");

      boolean nextUpper = false;
      for (int i = 0; i < name.length(); i++)
      {
         char next = name.charAt(i);
         if (next == ' ' || next == '_')
         {
            nextUpper = true;
            continue;
         }

         if (buff.length() == 0 && // 
               !(Character.isAlphabetic(next)// 
                     || next == '$'))//OK $ is a valid initial character in a JS identifier but seriously why dude, just why? 
         {
            next = 'x';
         }

         if (nextUpper)
         {
            next = Character.toUpperCase(next);
            nextUpper = false;
         }

         if (buff.length() == 0)
            next = Character.toLowerCase(next);

         buff.append(next);
      }
      return buff.toString();
   }

   protected String makeRelationshipUniqueName(Collection resource, Relationship rel)
   {
      String name = null;
      String type = rel.getType();
      boolean pluralize = false;
      if (type.equals(Relationship.REL_MANY_TO_ONE))
      {
         name = rel.getFk1Col1().getColumnName();
         if (name.toLowerCase().endsWith("id") && name.length() > 2)
         {
            name = name.substring(0, name.length() - 2);
         }
      }
      else if (type.equals(Relationship.REL_ONE_TO_MANY))
      {
         //Example
         //
         //if the Alarm table has a FK to the Category table
         //this would be called to add a relationship to the Category
         //collection called "alarms"....this is the default case
         //assuming the Alarm fk column is semantically related to 
         //the Category table with a name such as:
         //category, categories, categoryId or categoriesId 
         //
         //say for example that the Alarm table had two foreign
         //keys to the Category table.  One called "categoryId"
         //and the other called "subcategoryId".  In this case
         //the "categoryId" column is semantically related and would
         //result in the collection property "alarms" being added
         //to the Category collection.  The "subcategoyId" column
         //name is not one of the semantically related names 
         //so it results in a property called "subcategoryAlarms"
         //being added to the Category collection.

         String idxColName = rel.getFk1Col1().getColumnName();
         if (idxColName.toLowerCase().endsWith("id") && idxColName.length() > 2)
         {
            idxColName = idxColName.substring(0, idxColName.length() - 2);
         }

         idxColName = idxColName.replace("_", "");//allow fk cols like "person_id"
         if (idxColName.toUpperCase().equals(idxColName))
            idxColName = idxColName.toLowerCase();

         String collectionName = resource.getName();
         String relatedCollectionName = rel.getRelated().getName();
         //String tableName = resource.getTable().getName();
         if (!collectionName.equalsIgnoreCase(idxColName) //
               && !Pluralizer.plural(idxColName).equalsIgnoreCase(collectionName))
         {
            name = idxColName + Character.toUpperCase(relatedCollectionName.charAt(0)) + relatedCollectionName.substring(1, relatedCollectionName.length());
            //System.out.println("RELATIONSHIP: " + name + " " + rel);
         }
         else
         {
            name = relatedCollectionName;
         }

         pluralize = true;
      }
      else if (type.equals(Relationship.REL_MANY_TO_MANY))
      {
         name = rel.getFk2Col1().getPk().getCollection().getTableName();
         pluralize = true;
      }

      name = beautifyAttributeName(name);

      if (pluralize)
      {
         name = Pluralizer.plural(name);
      }

      return name;
   }

   protected String makeRelationshipName(Collection resource, Relationship rel)
   {
      String name = null;
      String type = rel.getType();
      boolean pluralize = false;
      if (type.equals(Relationship.REL_MANY_TO_ONE))
      {
         name = rel.getFk1Col1().getJsonName();
         if (name.toLowerCase().endsWith("id") && name.length() > 2)
         {
            name = name.substring(0, name.length() - 2);
         }
      }
      else if (type.equals(Relationship.REL_ONE_TO_MANY))
      {
         name = rel.getRelated().getName();
         pluralize = true;
      }
      else if (type.equals(Relationship.REL_MANY_TO_MANY))
      {
         name = rel.getFk2Col1().getPk().getCollection().getName();
         pluralize = true;
      }

      name = beautifyAttributeName(name);

      if (pluralize)
      {
         name = Pluralizer.plural(name);
      }

      return name;
   }

   public Object cast(Property column, Object value)
   {
      return Utils.cast(column != null ? column.getType() : null, value);
   }

   public Object cast(String type, Object value)
   {
      return Utils.cast(type, value);
   }

   public Set<Term> mapToColumns(Collection collection, Term term)
   {
      Set<Term> terms = new HashSet();

      if (term.getParent() == null)
         terms.add(term);

      if (collection == null)
         return terms;

      if (term.isLeaf() && !term.isQuoted())
      {
         String token = term.getToken();

         while (token.startsWith("-") || token.startsWith("+"))
            token = token.substring(1, token.length());

         Property attr = collection.findProperty(token);
         if (attr != null)
         {
            String columnName = attr.getColumnName();

            if (term.getToken().startsWith("-"))
               columnName = "-" + columnName;
            term.withToken(columnName);
         }
      }
      else
      {
         for (Term child : term.getTerms())
         {
            terms.addAll(mapToColumns(collection, child));
         }
      }

      return terms;
   }

   public boolean isStarted()
   {
      return started;
   }

   public boolean isShutdown()
   {
      return shutdown;
   }

   public Property getProperty(String table, String col)
   {
      for (Collection t : collections)
      {
         if (t.getTableName().equalsIgnoreCase(table))
         {
            for (Property c : t.getProperties())
            {
               if (c.getColumnName().equalsIgnoreCase(col))
               {
                  return c;
               }
            }

            return null;
         }
      }
      return null;
   }

   public Collection getCollectionByTableName(String tableName)
   {
      for (Collection t : collections)
      {
         if (tableName.equalsIgnoreCase(t.getTableName()))
            return t;
      }

      return null;
   }

   public void removeCollection(Collection table)
   {
      collections.remove(table);
   }

   /**
    * @return the collections
    */
   public List<Collection> getCollections()
   {
      return collections;
   }

   public T withIncludeTables(String includeTables)
   {
      for (String pair : Utils.explode(",", includeTables))
      {
         String tableName = pair.indexOf('|') < 0 ? pair : pair.substring(0, pair.indexOf("|"));
         String collectionName = pair.indexOf('|') < 0 ? pair : pair.substring(pair.indexOf("|") + 1);
         this.includeTables.put(tableName, collectionName);
      }
      return (T) this;
   }

   /**
    * @param collections to include (add not replace)
    */
   public T withCollections(Collection... colls)
   {
      for (Collection coll : colls)
         withCollection(coll);

      return (T) this;
   }

   public T withCollection(Collection tbl)
   {
      if (tbl != null)
      {
         if (tbl.getDb() != this)
            tbl.withDb(this);

         if (!collections.contains(tbl))
            collections.add(tbl);
      }
      return (T) this;
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
   public T withName(String name)
   {
      this.name = name;
      return (T) this;
   }

   public boolean isType(String... types)
   {
      String type = getType();
      if (type == null)
         return false;

      for (String t : types)
      {
         if (type.equalsIgnoreCase(t))
            return true;
      }
      return false;
   }

   public String getType()
   {
      return type;
   }

   public T withType(String type)
   {
      this.type = type;
      return (T) this;
   }

   public boolean isBootstrap()
   {
      return bootstrap;
   }

   public T withBootstrap(boolean bootstrap)
   {
      this.bootstrap = bootstrap;
      return (T) this;
   }

   public Path getEndpointPath()
   {
      return endpointPath;
   }

   public T withEndpointPath(Path endpointPath)
   {
      this.endpointPath = endpointPath;
      return (T) this;
   }

   /*
    * Copyright 2011 Atteo.
    * 
    * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
    * in compliance with the License. You may obtain a copy of the License at
    * 
    * http://www.apache.org/licenses/LICENSE-2.0
    * 
    * Unless required by applicable law or agreed to in writing, software distributed under the License
    * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
    * or implied. See the License for the specific language governing permissions and limitations under
    * the License.
    */
   /**
    * Transforms English words from singular to plural form.
    * <p>
    * Examples:
    * <pre>
    *    English.plural("word") = "words";
    *
    *    English.plural("cat", 1) = "cat";
    *    English.plural("cat", 2) = "cats";
    * </pre>
    * </p>
    * <p>
    * Based on <a href="http://www.csse.monash.edu.au/~damian/papers/HTML/Plurals.html">
    * An Algorithmic Approach to English Pluralization</a> by Damian Conway.
    * </p>
    */
   static class Pluralizer
   {
      enum MODE {
         ENGLISH_ANGLICIZED, ENGLISH_CLASSICAL
      }

      private static final String[] CATEGORY_EX_ICES  = {"codex", "murex", "silex",};

      private static final String[] CATEGORY_IX_ICES  = {"radix", "helix",};

      private static final String[] CATEGORY_UM_A     = {"bacterium", "agendum", "desideratum", "erratum", "stratum", "datum", "ovum", "extremum", "candelabrum",};

      // Always us -> i
      private static final String[] CATEGORY_US_I     = {"alumnus", "alveolus", "bacillus", "bronchus", "locus", "nucleus", "stimulus", "meniscus", "thesaurus",};

      private static final String[] CATEGORY_ON_A     = {"criterion", "perihelion", "aphelion", "phenomenon", "prolegomenon", "noumenon", "organon", "asyndeton", "hyperbaton",};

      private static final String[] CATEGORY_A_AE     = {"alumna", "alga", "vertebra", "persona"};

      // Always o -> os
      private static final String[] CATEGORY_O_OS     = {"albino", "archipelago", "armadillo", "commando", "crescendo", "fiasco", "ditto", "dynamo", "embryo", "ghetto", "guano", "inferno", "jumbo", "lumbago", "magneto", "manifesto", "medico", "octavo", "photo", "pro", "quarto", "canto", "lingo", "generalissimo", "stylo", "rhino", "casino", "auto", "macro", "zero", "todo"};

      // Classical o -> i  (normally -> os)
      private static final String[] CATEGORY_O_I      = {"solo", "soprano", "basso", "alto", "contralto", "tempo", "piano", "virtuoso",};

      private static final String[] CATEGORY_EN_INA   = {"stamen", "foramen", "lumen"};

      // -a to -as (anglicized) or -ata (classical)
      private static final String[] CATEGORY_A_ATA    = {"anathema", "enema", "oedema", "bema", "enigma", "sarcoma", "carcinoma", "gumma", "schema", "charisma", "lemma", "soma", "diploma", "lymphoma", "stigma", "dogma", "magma", "stoma", "drama", "melisma", "trauma", "edema", "miasma"};

      private static final String[] CATEGORY_IS_IDES  = {"iris", "clitoris"};

      // -us to -uses (anglicized) or -us (classical)
      private static final String[] CATEGORY_US_US    = {"apparatus", "impetus", "prospectus", "cantus", "nexus", "sinus", "coitus", "plexus", "status", "hiatus"};

      private static final String[] CATEGORY_NONE_I   = {"afreet", "afrit", "efreet"};

      private static final String[] CATEGORY_NONE_IM  = {"cherub", "goy", "seraph"};

      private static final String[] CATEGORY_EX_EXES  = {"apex", "latex", "vertex", "cortex", "pontifex", "vortex", "index", "simplex"};

      private static final String[] CATEGORY_IX_IXES  = {"appendix"};

      private static final String[] CATEGORY_S_ES     = {"acropolis", "chaos", "lens", "aegis", "cosmos", "mantis", "alias", "dais", "marquis", "asbestos", "digitalis", "metropolis", "atlas", "epidermis", "pathos", "bathos", "ethos", "pelvis", "bias", "gas", "polis", "caddis", "glottis", "rhinoceros", "cannabis", "glottis", "sassafras", "canvas", "ibis", "trellis"};

      private static final String[] CATEGORY_MAN_MANS = {"human", "Alabaman", "Bahaman", "Burman", "German", "Hiroshiman", "Liman", "Nakayaman", "Oklahoman", "Panaman", "Selman", "Sonaman", "Tacoman", "Yakiman", "Yokohaman", "Yuman"};

      private static Pluralizer     inflector         = new Pluralizer();

      public Pluralizer()
      {
         this(MODE.ENGLISH_ANGLICIZED);
      }

      public Pluralizer(MODE mode)
      {

         uncountable(new String[]{
               // 2. Handle words that do not inflect in the plural (such as fish, travois, chassis, nationalities ending
               // endings
               "fish", "ois", "sheep", "deer", "pox", "itis",

               // words
               "bison", "flounder", "pliers", "bream", "gallows", "proceedings", "breeches", "graffiti", "rabies", "britches", "headquarters", "salmon", "carp", "herpes", "scissors", "chassis", "high-jinks", "sea-bass", "clippers", "homework", "series", "cod", "innings", "shears", "contretemps", "jackanapes", "species", "corps", "mackerel", "swine", "debris", "measles", "trout", "diabetes", "mews", "tuna", "djinn", "mumps", "whiting", "eland", "news", "wildebeest", "elk", "pincers", "sugar"});

         // 4. Handle standard irregular plurals (mongooses, oxen, etc.)

         irregular(new String[][]{{"child", "children"}, // classical
               {"ephemeris", "ephemerides"}, // classical
               {"mongoose", "mongoose"}, // anglicized
               {"mythos", "mythoi"}, // classical
               // TO DO: handle entire word correctly
               //{ "ox", "oxen" }, // classical
               {"soliloquy", "soliloquies"}, // anglicized
               {"trilby", "trilbys"}, // anglicized
               {"genus", "genera"}, // classical
               {"quiz", "quizzes"},});

         if (mode == MODE.ENGLISH_ANGLICIZED)
         {
            // Anglicized plural
            irregular(new String[][]{{"beef", "beefs"}, {"brother", "brothers"}, {"cow", "cows"}, {"genie", "genies"}, {"money", "moneys"}, {"octopus", "octopuses"}, {"opus", "opuses"},});
         }
         else if (mode == MODE.ENGLISH_CLASSICAL)
         {
            // Classical plural
            irregular(new String[][]{{"beef", "beeves"}, {"brother", "brethren"}, {"cos", "kine"}, {"genie", "genii"}, {"money", "monies"}, {"octopus", "octopodes"}, {"opus", "opera"},});
         }

         categoryRule(CATEGORY_MAN_MANS, "", "s");

         // questionable
         /*
          rule(new String[][] {
                 { "(ness)$", "$1" },
                 { "(ality)$", "$1" }
                 { "(icity)$", "$1" },
                 { "(ivity)$", "$1" },
         });
          */
         // 5. Handle irregular inflections for common suffixes
         rule(new String[][]{{"man$", "men"}, {"([lm])ouse$", "$1ice"}, {"tooth$", "teeth"}, {"goose$", "geese"}, {"foot$", "feet"}, {"zoon$", "zoa"}, {"([csx])is$", "$1es"},});

         // 6. Handle fully assimilated classical inflections
         categoryRule(CATEGORY_EX_ICES, "ex", "ices");
         categoryRule(CATEGORY_IX_ICES, "ix", "ices");
         categoryRule(CATEGORY_UM_A, "um", "a");
         categoryRule(CATEGORY_ON_A, "on", "a");
         categoryRule(CATEGORY_A_AE, "a", "ae");

         // 7. Handle classical variants of modern inflections
         if (mode == MODE.ENGLISH_CLASSICAL)
         {
            rule(new String[][]{{"trix$", "trices"}, {"eau$", "eaux"}, {"ieu$", "ieux"}, {"(..[iay])nx$", "$1nges"},});
            categoryRule(CATEGORY_EN_INA, "en", "ina");
            categoryRule(CATEGORY_A_ATA, "a", "ata");
            categoryRule(CATEGORY_IS_IDES, "is", "ides");
            categoryRule(CATEGORY_US_US, "", "");
            categoryRule(CATEGORY_O_I, "o", "i");
            categoryRule(CATEGORY_NONE_I, "", "i");
            categoryRule(CATEGORY_NONE_IM, "", "im");
            categoryRule(CATEGORY_EX_EXES, "ex", "ices");
            categoryRule(CATEGORY_IX_IXES, "ix", "ices");
         }

         categoryRule(CATEGORY_US_I, "us", "i");

         rule("([cs]h|[zx])$", "$1es");
         categoryRule(CATEGORY_S_ES, "", "es");
         categoryRule(CATEGORY_IS_IDES, "", "es");
         categoryRule(CATEGORY_US_US, "", "es");
         rule("(us)$", "$1es");
         categoryRule(CATEGORY_A_ATA, "", "s");

         // The suffixes -ch, -sh, and -ss all take -es in the plural (churches,
         // classes, etc)...
         rule(new String[][]{{"([cs])h$", "$1hes"}, {"ss$", "sses"}});

         // Certain words ending in -f or -fe take -ves in the plural (lives,
         // wolves, etc)...
         rule(new String[][]{{"([aeo]l)f$", "$1ves"}, {"([^d]ea)f$", "$1ves"}, {"(ar)f$", "$1ves"}, {"([nlw]i)fe$", "$1ves"}});

         // Words ending in -y take -ys
         rule(new String[][]{{"([aeiou])y$", "$1ys"}, {"y$", "ies"},});

         // Some words ending in -o take -os (including does preceded by a vowel)
         categoryRule(CATEGORY_O_I, "o", "os");
         categoryRule(CATEGORY_O_OS, "o", "os");
         rule("([aeiou])o$", "$1os");
         // The rest take -oes
         rule("o$", "oes");

         rule("ulum", "ula");

         categoryRule(CATEGORY_A_ATA, "", "es");

         rule("s$", "ses");
         // Otherwise, assume that the plural just adds -s
         rule("$", "s");
      }

      //   /**
      //    * Returns plural form of the given word.
      //    *
      //    * @param word word in singular form
      //    * @return plural form of the word
      //    */
      //   @Override
      //   public String getPlural(String word)
      //   {
      //      return super.getPlural(word);
      //   }

      /**
       * Returns singular or plural form of the word based on count.
       *
       * @param word word in singular form
       * @param count word count
       * @return form of the word correct for given count
       */
      public String getPlural(String word, int count)
      {
         if (count == 1)
         {
            return word;
         }
         return getPlural(word);
      }

      /**
       * Returns plural form of the given word.
       * <p>
       * For instance:
       * <pre>
       * {@code
       * English.plural("cat") == "cats";
       * }
       * </pre>
       * </p>
       * @param word word in singular form
       * @return plural form of given word
       */
      public static String plural(String word)
      {
         if (word.endsWith("s") || word.endsWith("S"))
            return word;

         word = inflector.getPlural(word);
         return word;
      }

      /**
       * Returns singular or plural form of the word based on count.
       * <p>
       * For instance:
       * <pre>
       * {@code
       * English.plural("cat", 1) == "cat";
       * English.plural("cat", 2) == "cats";
       * }
       * </pre>
       * </p>
       * @param word word in singular form
       * @param count word count
       * @return form of the word correct for given count
       */
      public static String plural(String word, int count)
      {
         return inflector.getPlural(word, count);
      }

      public static void setMode(MODE mode)
      {
         Pluralizer newInflector = new Pluralizer(mode);
         inflector = newInflector;
      }

      //   public static abstract class TwoFormInflector
      //   {
      private interface Rule
      {
         String getPlural(String singular);
      }

      private static class RegExpRule implements Rule
      {
         private final Pattern singular;
         private final String  plural;

         private RegExpRule(Pattern singular, String plural)
         {
            this.singular = singular;
            this.plural = plural;
         }

         @Override
         public String getPlural(String word)
         {
            StringBuffer buffer = new StringBuffer();
            Matcher matcher = singular.matcher(word);
            if (matcher.find())
            {
               matcher.appendReplacement(buffer, plural);
               matcher.appendTail(buffer);
               return buffer.toString();
            }
            return null;
         }
      }

      private static class CategoryRule implements Rule
      {
         private final String[] list;
         private final String   singular;
         private final String   plural;

         public CategoryRule(String[] list, String singular, String plural)
         {
            this.list = list;
            this.singular = singular;
            this.plural = plural;
         }

         @Override
         public String getPlural(String word)
         {
            String lowerWord = word.toLowerCase();
            for (String suffix : list)
            {
               if (lowerWord.endsWith(suffix))
               {
                  if (!lowerWord.endsWith(singular))
                  {
                     throw new RuntimeException("Internal error");
                  }
                  return word.substring(0, word.length() - singular.length()) + plural;
               }
            }
            return null;
         }
      }

      private final List<Rule> rules = new ArrayList<Rule>();

      protected String getPlural(String word)
      {
         for (Rule rule : rules)
         {
            String result = rule.getPlural(word);
            if (result != null)
            {
               return result;
            }
         }
         return null;
      }

      protected void uncountable(String[] list)
      {
         rules.add(new CategoryRule(list, "", ""));
      }

      protected void irregular(String singular, String plural)
      {
         if (singular.charAt(0) == plural.charAt(0))
         {
            rules.add(new RegExpRule(Pattern.compile("(?i)(" + singular.charAt(0) + ")" + singular.substring(1) + "$"), "$1" + plural.substring(1)));
         }
         else
         {
            rules.add(new RegExpRule(Pattern.compile(Character.toUpperCase(singular.charAt(0)) + "(?i)" + singular.substring(1) + "$"), Character.toUpperCase(plural.charAt(0)) + plural.substring(1)));
            rules.add(new RegExpRule(Pattern.compile(Character.toLowerCase(singular.charAt(0)) + "(?i)" + singular.substring(1) + "$"), Character.toLowerCase(plural.charAt(0)) + plural.substring(1)));
         }
      }

      protected void irregular(String[][] list)
      {
         for (String[] pair : list)
         {
            irregular(pair[0], pair[1]);
         }
      }

      protected void rule(String singular, String plural)
      {
         rules.add(new RegExpRule(Pattern.compile(singular, Pattern.CASE_INSENSITIVE), plural));
      }

      protected void rule(String[][] list)
      {
         for (String[] pair : list)
         {
            rules.add(new RegExpRule(Pattern.compile(pair[0], Pattern.CASE_INSENSITIVE), pair[1]));
         }
      }

      protected void categoryRule(String[] list, String singular, String plural)
      {
         rules.add(new CategoryRule(list, singular, plural));
      }
   }

}
