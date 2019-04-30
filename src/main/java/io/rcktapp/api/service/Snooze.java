/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * http://rocketpartners.io
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.rcktapp.api.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.sql.DataSource;

import org.atteo.evo.inflector.English;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import io.forty11.j.J;
import io.forty11.js.JS;
import io.forty11.js.JSArray;
import io.forty11.js.JSObject;
import io.forty11.sql.Sql;
import io.rcktapp.api.Api;
import io.rcktapp.api.ApiException;
import io.rcktapp.api.Attribute;
import io.rcktapp.api.Col;
import io.rcktapp.api.Collection;
import io.rcktapp.api.Db;
import io.rcktapp.api.Entity;
import io.rcktapp.api.Handler;
import io.rcktapp.api.Relationship;
import io.rcktapp.api.Rule;
import io.rcktapp.api.SC;
import io.rcktapp.api.Tbl;
import io.rcktapp.utils.AutoWire;

/**
 * Servlet implementation class RestService
 *
 * Notes on REST verbs and status code responses:
 * ------------------------------------------
 * -- TODO
 * ------------------------------------------
 *  
 *  - GET allow filter params to involve joined tables
 *      - may need to left join all FKs to the root query to make it work

 *  - GET lastmodified & eTag caching
 *  - GET/PUT/POST eTag header
 *  - DELETE
 *  - PUT
 *  - Nested PUT/POST
 *  
 *  - Security
 *  - Rate limiting
 *  - Connection pool, hold / reuse connections.
 *  - lastModified column Support
 *  - support for "deleted" column
 *  - object version support
 *  - full logging w/ rollback
 *  -
 *  - DONE - GET expansion control
 *  - DONE - GET 404 in resource not found
 *  - DONE - GET with comma seperated list of ids
 *  - DONE - Make everything case insensative
 *  - DONE - GET paging/sorting
 *  - DONE - GET query term parser
 *  - DONE - Many 2 Many POST
 *  - DONE - 201 w/ Location header response
 *  - DONE - Auto bootstrap DB/API w/ API cache, only store exceptions in DB
 *  - DONE - batch post
 *  - DONE - GZIP support
 * 
 * 
 * ------------------------------------------
 * @see http://www.vinaysahni.com/best-practices-for-a-pragmatic-restful-api
 * @see http://www.restapitutorial.com/lessons/httpmethods.html
 *
 * @see http://blog.mwaysolutions.com/2014/06/05/10-best-practices-for-better-restful-api/
 * @see http://stackoverflow.com/questions/12806386/standard-json-api-response-format
 * @see https://stormpath.com/blog/linking-and-resource-expansion-rest-api-tips/
 *
 * http://restful-api-design.readthedocs.org/en/latest/intro.html
 *
 * http://stackoverflow.com/questions/297005/what-is-the-gold-standard-for-website-apis-twitter-flickr-facebook-etc
 *
 * https://stripe.com/docs/api/curl#intro
 * http://samplacette.com/five-json-rest-api-link-formats-compared/
 * https://www.mnot.net/blog/2011/11/25/linking_in_json
 * http://stateless.co/hal_specification.html
 * http://tools.ietf.org/html/draft-kelly-json-hal-03
 * https://blog.safaribooksonline.com/2013/05/23/instrumenting-apis-with-links-in-rest/
 *
 * Error Codes:
 *      http://msdn.microsoft.com/en-us/library/azure/dd179357.aspx
 *      http://www.restapitutorial.com/httpstatuscodes.html
 *
 * http://www.restapitutorial.com/media/RESTful_Best_Practices-v1_1.pdf
 *
 * odata endpoint filters: http://msdn.microsoft.com/en-us/library/gg309461.aspx
 *
 * http://bitoftech.net/2014/01/13/tutorial-for-building-spa-using-angularjs-example/
 * http://bitoftech.net/2014/01/13/building-spa-using-angularjs-part-1/
 *
 * https://spring.io/guides/gs/consuming-rest-angularjs/
 * http://patrickgrimard.com/2014/05/16/pagination-with-spring-data-and-hateoas-in-an-angularjs-app/
 * https://florian.voutzinos.com/blog/data-tables-with-symfony-angular/
 * http://patrickgrimard.com/2014/05/16/pagination-with-spring-data-and-hateoas-in-an-angularjs-app/
 * http://ianbattersby.github.io/blog/2013/09/05/5-days-of-simple-web-hateoas/
 *
 */
public class Snooze extends Service
{
   public static Snooze           snooze        = null;

   int                            MIN_POOL_SIZE = 3;
   int                            MAX_POOL_SIZE = 10;

   DataSource                     ds            = null;
   Map<Db, ComboPooledDataSource> pools         = new HashMap();

   public Snooze() throws Exception
   {
      snooze = this;
      //cache = new CacheApi(this);
   }

   @Override
   public void init(ServletConfig config) throws ServletException
   {
      super.init(config);

      try
      {
         ServletContext cx = getServletContext();

         loadProperties(cx);
         loadJson(new File(cx.getRealPath("WEB-INF/snooze")));

         for (Api a : getApis())
         {
            for (Db d : a.getDbs())
            {
               Class.forName(d.getDriver()).newInstance();
               bootstrapDb(d);
               bootstrapApi(a, d);
            }
         }
      }
      catch (Exception e)
      {
         throw new ServletException("Error loading configuration", e);
      }
   }

   public void loadJson(File file)
   {
      if (!file.exists())
         return;

      if (file.isDirectory() && !file.isHidden())
      {
         File[] files = file.listFiles();
         for (int i = 0; files != null && i < files.length; i++)
         {
            loadJson(files[i]);
         }
      }
      else if (file.getName().endsWith(".json"))
      {
         System.out.println("loading:" + file);
         JSObject json = JS.toJSObject(J.read(file));
         loadJson(json);
      }
   }

   public void loadJson(JSObject json)
   {
      JSArray apis = json.getArray("apis");
      for (int i = 0; apis != null && i < apis.length(); i++)
      {
         parseApis(apis);
      }

      JSArray rules = json.getArray("rules");
      for (int i = 0; rules != null && i < rules.length(); i++)
      {
         parseRules(rules);
      }

      JSArray dbs = json.getArray("dbs");
      for (int i = 0; dbs != null && i < dbs.length(); i++)
      {
         parseDbs(dbs);
      }

   }

   protected List<Api> parseApis(JSArray arr)
   {
      List<Api> apis = new ArrayList();

      for (JSObject api : (List<JSObject>) arr.asList())
      {
         String name = api.getString("name");
         Api a = getApi(name);
         if (a == null)
         {
            a = new Api(name);
            addApi(a);
         }

         a.setHandlers(api.getString("handlers"));
         a.setUrls(api.getString("urls"));

         List<Db> dbs = parseDbs(api.getArray("dbs"));
         for (Db db : dbs)
         {
            a.addDb(db);
         }

         List<Rule> rules = parseRules(api.getArray("rules"));
         for (Rule rule : rules)
         {
            a.addRule(rule);
         }
      }

      return apis;
   }

   protected List<Rule> parseRules(JSArray arr)
   {
      List<Rule> rules = new ArrayList();

      for (int i = 0; rules != null && i < arr.length(); i++)
      {
         JSObject rule = arr.getObject(i);
         String rn = rule.getString("name");

         Rule r = new Rule(rn);
         rules.add(r);

         r.setMethods(rule.getString("methods"));
         r.setPaths(rule.getString("paths"));
         //r.setReferrers(rule.getString("referrers"));
         r.setParams(rule.getString("params"));
         r.setNegate("true".equalsIgnoreCase(rule.get("negate") + ""));

         String order = rule.getString("order");
         if (order != null)
            r.setOrder(Float.parseFloat(order));

         String priority = rule.getString("priority");
         if (priority != null)
            r.setPriority(Float.parseFloat(priority));

         r.setHandler(rule.getString("handler"));
         r.setStatus(rule.getString("status"));
         r.setTerminate("true".equalsIgnoreCase(rule.getString("terminate") + ""));
         r.setConfig(rule.getObject("config"));
         r.setPerms(rule.getString("perms"));
         r.setRoles(rule.getString("roles"));

         String apiName = rule.getString("apis");
         apiName = apiName != null ? apiName : rule.getString("api");

         if (apiName != null)
         {
            for (String name : apiName.split(","))
            {
               name = name.trim();
               if (!J.empty(name))
               {
                  Api api = getApi(name);
                  if (api != null)
                     api.addRule(r);
                  else
                     throw new RuntimeException("Invalid configuration, api does not exist: " + name + "\r\n" + arr);
               }
            }
         }
      }

      return rules;
   }

   protected List<Db> parseDbs(JSArray arr)
   {
      List<Db> dbs = new ArrayList();

      for (int i = 0; arr != null && i < arr.length(); i++)
      {
         JSObject db = arr.getObject(i);
         String dbn = db.getString("name");

         Api api = null;
         String apiName = db.getString("api");
         if (apiName != null)
         {
            api = getApi(apiName);
         }

         Db d = api != null ? api.getDb(dbn) : null;
         if (d == null)
         {
            d = new Db(dbn);
            if (api != null)
               api.addDb(d);
         }
         dbs.add(d);

         d.setDriver(db.getString("driver"));
         d.setUrl(db.getString("url"));
         d.setUser(db.getString("user"));
         d.setPass(db.getString("pass"));
         //d.setSchema(db.getString("schema"));

         String minPool = db.getString("minPool");
         if (minPool != null)
            d.setPoolMin(Integer.parseInt(minPool));

         String maxPool = db.getString("maxPool");
         if (maxPool != null)
            d.setPoolMax(Integer.parseInt(maxPool));
      }

      return dbs;
   }

   public void loadProperties(ServletContext cx) throws Exception
   {
      Properties props = new Properties();

      for (int i = -1; i <= 100; i++)
      {
         String fileName = "/WEB-INF/snooze" + (i < 0 ? "" : i) + ".properties";
         InputStream is = getServletContext().getResourceAsStream(fileName);
         if (is != null)
         {
            System.out.println("loading: " + fileName);
            props.load(is);
         }
      }

      String profile = System.getProperty("snooze.profile");
      if (profile == null)
         profile = System.getenv("snooze.profile");

      if (profile != null)
      {
         for (int i = -1; i <= 100; i++)
         {
            String fileName = "/WEB-INF/snooze" + (i < 0 ? "" : i) + "-" + profile + ".properties";
            InputStream is = getServletContext().getResourceAsStream(fileName);
            if (is != null)
            {
               System.out.println("loading: " + fileName);
               props.load(is);
            }
         }
      }

      setDebug(Boolean.parseBoolean(props.getProperty("debug", "false")));

      if (!J.empty(props.getProperty("driver")))
      {
         ComboPooledDataSource cpds = new ComboPooledDataSource();

         cpds.setDriverClass(props.getProperty("driver"));
         cpds.setJdbcUrl(props.getProperty("url"));
         cpds.setUser(props.getProperty("user"));
         cpds.setPassword(props.getProperty("pass"));
         cpds.setMinPoolSize(Integer.parseInt(props.getProperty("poolMin", MIN_POOL_SIZE + "")));
         cpds.setMaxPoolSize(Integer.parseInt(props.getProperty("poolMax", MAX_POOL_SIZE + "")));

         ds = cpds;
      }

      AutoWire wire = new AutoWire()
         {
            @Override
            public void onLoad(String name, Object module, Map<String, Object> props) throws Exception
            {
               Field field = getField("name", module.getClass());
               if (field != null)
                  field.set(module, name);

               if (module instanceof Handler)
                  addHandler(name, (Handler) module);

               if (module instanceof Api)
                  addApi((Api) module);
            }
         };
      wire.load(props);

   }

   public Connection getConnection(Api api) throws ApiException
   {
      return getConnection(api, null);
   }

   public Connection getConnection(String url, String collectionKey) throws ApiException
   {
      ApiMatch match = findApi(url);
      if (match != null)
         return getConnection(match.api, collectionKey);
      return null;
   }

   public Connection getConnection(Api api, String collectionKey) throws ApiException
   {
      try
      {
         Db db = null;

         if (collectionKey != null)
         {
            db = api.findDb(collectionKey);
         }

         if (db == null)
            db = api.getDbs().get(0);

         ComboPooledDataSource pool = pools.get(db);

         if (pool == null)
         {
            synchronized (this)
            {
               pool = pools.get(db);

               if (pool == null)
               {
                  String driver = db.getDriver();
                  String url = db.getUrl();
                  String user = db.getUser();
                  String password = db.getPass();
                  int minPoolSize = db.getPoolMin();
                  int maxPoolSize = db.getPoolMax();

                  minPoolSize = Math.max(MIN_POOL_SIZE, minPoolSize);
                  maxPoolSize = Math.min(maxPoolSize, MAX_POOL_SIZE);

                  pool = new ComboPooledDataSource();
                  pool.setDriverClass(driver);
                  pool.setJdbcUrl(url);
                  pool.setUser(user);
                  pool.setPassword(password);
                  pool.setMinPoolSize(minPoolSize);
                  pool.setMaxPoolSize(maxPoolSize);

                  pools.put(db, pool);
               }
            }
         }

         return pool.getConnection();
      }
      catch (Exception ex)
      {
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, "Unable to get DB connection", ex);
      }
   }

   public Connection getConnection() throws Exception
   {
      if (ds != null)
      {
         return ds.getConnection();
      }
      return null;
   }

   /**
    * Overridden to lazy load APIs from the DB
    */
   @Override
   public synchronized ApiMatch findApi(String url) throws ApiException
   {
      ApiMatch api = super.findApi(url);
      if (api == null)
      {
         api = loadApi(url);
         if (api != null)
            addApi(api.api);
      }
      return api;
   }

   /**
    * Returns a completely new copy of the API loaded from the DB(s).  There
    * is no caching in this method.  This will cause the API to auto bootstrap 
    * if there are no collections and will store the api 
    */
   ApiMatch loadApi(String url) throws ApiException
   {
      Connection conn = null;
      Api api = null;
      String apiUrl = null;

      try
      {
         conn = getConnection();
         if (conn != null)
         {

            if (url.indexOf('?') > 0)
               url = url.substring(0, url.indexOf('?'));

            int afterHostSlash = url.indexOf('/', 8);
            apiUrl = afterHostSlash > 0 ? url.substring(0, afterHostSlash + 1) : url;
            String[] path = afterHostSlash > 0 ? url.substring(afterHostSlash + 1, url.length()).split("/") : new String[0];

            String sql = "SELECT a.* FROM Api a JOIN Url u ON a.id = u.apiId WHERE u.url = ? LIMIT 1";

            for (int i = 0; i <= path.length; i++)
            {
               if (i > 0)
                  apiUrl = apiUrl + path[i - 1] + "/";

               api = (Api) Sql.selectObject(conn, sql, new Api(), apiUrl);

               if (api.getId() <= 0)
                  api = null;

               if (api != null)
                  break;
            }

            if (api == null)
            {
               throw new ApiException(SC.SC_400_BAD_REQUEST, "Unable to find an API for url: '" + url + "'");
            }

            List<String> urls = Sql.selectList(conn, "SELECT url FROM Url WHERE apiId = ?", api.getId());
            api.setUrls(urls);

            sql = "";
            sql += " SELECT d.* FROM Db d ";
            sql += " JOIN ApiDbs ad ON ad.dbId = d.id ";
            sql += " JOIN Api a ON ad.apiId = a.id ";
            sql += " WHERE a.id = ? ";

            List vars = new ArrayList();
            vars.add(api.getId());

            List<Db> dbs = Sql.selectObjects(conn, sql, Db.class, vars.toArray());
            api.setDbs(dbs);

            for (Db db : dbs)
            {
               bootstrapDb(db);
               bootstrapApi(api, db);
            }

            List<Rule> rules = Sql.selectObjects(conn, "SELECT * FROM Rule WHERE apiId = ?", Rule.class, api.getId());

            if (rules.size() > 0)
            {
               api.setRules(rules);
            }
            //            else
            //            {
            //               String paths = "";
            //               for (Collection c : api.getCollections())
            //               {
            //                  paths += c.getName() + "/*, ";
            //               }
            //
            //               api.addRule(new Rule("GET", paths, get.getName()));
            //               api.addRule(new Rule("POST", paths, post.getName()));
            //               api.addRule(new Rule("PUT", paths, post.getName()));
            //               api.addRule(new Rule("DELETE", paths, delete.getName()));
            //
            //               Sql.execute(conn, "INSERT INTO Rule (tenantId, apiId, `order`, priority, methods, paths, handler) VALUES (?,?,?,?,?,?,?)", api.getTenantId(), api.getId(), Rule.DEFAULT_ORDER, Rule.DEFAULT_PRIORITY, "GET", paths, get.getName());
            //               Sql.execute(conn, "INSERT INTO Rule (tenantId, apiId, `order`, priority, methods, paths, handler) VALUES (?,?,?,?,?,?,?)", api.getTenantId(), api.getId(), Rule.DEFAULT_ORDER, Rule.DEFAULT_PRIORITY, "POST", paths, post.getName());
            //               Sql.execute(conn, "INSERT INTO Rule (tenantId, apiId, `order`, priority, methods, paths, handler) VALUES (?,?,?,?,?,?,?)", api.getTenantId(), api.getId(), Rule.DEFAULT_ORDER, Rule.DEFAULT_PRIORITY, "PUT", paths, post.getName());
            //               Sql.execute(conn, "INSERT INTO Rule (tenantId, apiId, `order`, priority, methods, paths, handler) VALUES (?,?,?,?,?,?,?)", api.getTenantId(), api.getId(), Rule.DEFAULT_ORDER, Rule.DEFAULT_PRIORITY, "DELETE", paths, delete.getName());
            //            }
         }
      }
      catch (Exception ex)
      {
         throw new ApiException(SC.SC_500_INTERNAL_SERVER_ERROR, ex.getMessage());
      }
      finally
      {
         Sql.close(conn);
      }

      if (api != null)
         return new ApiMatch(api, apiUrl);

      return null;
   }

   public static void bootstrapDb(Db db) throws Exception
   {
      Class.forName(db.getDriver());
      Connection apiConn = DriverManager.getConnection(db.getUrl(), db.getUser(), db.getPass());

      try
      {

         DatabaseMetaData dbmd = apiConn.getMetaData();

         //-- only here to map jdbc type integer codes to strings ex "4" to "BIGINT" or whatever it is
         Map<String, String> types = new HashMap<String, String>();
         for (Field field : Types.class.getFields())
         {
            types.put(field.get(null) + "", field.getName());
         }
         //--

         //-- the first loop through is going to construct all of the
         //-- Tbl and Col objects.  There will be a second loop through
         //-- that caputres all of the foreign key relationships.  You
         //-- have to do the fk loop second becuase the reference pk
         //-- object needs to exist so that it can be set on the fk Col
         ResultSet rs = dbmd.getTables(null, null, null, new String[]{"TABLE"});
         while (rs.next())
         {
            String tableCat = rs.getString("TABLE_CAT");
            String tableSchem = rs.getString("TABLE_SCHEM");
            String tableName = rs.getString("TABLE_NAME");
            String tableType = rs.getString("TABLE_TYPE");

            Tbl table = new Tbl(db, tableName);
            db.addTbl(table);

            ResultSet colsRs = dbmd.getColumns(tableCat, tableSchem, tableName, null);

            while (colsRs.next())
            {
               String colName = colsRs.getString("COLUMN_NAME");
               Object type = colsRs.getString("DATA_TYPE");
               String colType = types.get(type);

               Col column = new Col(table, colName, colType);
               table.addCol(column);

               //               if (DELETED_FLAGS.contains(colName.toLowerCase()))
               //               {
               //                  table.setDeletedFlag(column);
               //               }
            }
            colsRs.close();

            ResultSet indexMd = dbmd.getIndexInfo(apiConn.getCatalog(), null, tableName, true, false);
            while (indexMd.next())
            {
               String colName = indexMd.getString("COLUMN_NAME");
               Col col = db.getCol(tableName, colName);
               col.setUnique(true);
            }
            indexMd.close();

         }
         rs.close();

         //-- now link all of the fks to pks
         //-- this is done after the first loop
         //-- so that all of the tbls/cols are
         //-- created first and are there to
         //-- be connected
         rs = dbmd.getTables(null, null, null, new String[]{"TABLE"});
         while (rs.next())
         {
            String tableName = rs.getString("TABLE_NAME");

            ResultSet keyMd = dbmd.getImportedKeys(apiConn.getCatalog(), null, tableName);
            while (keyMd.next())
            {
               String fkTableName = keyMd.getString("FKTABLE_NAME");
               String fkColumnName = keyMd.getString("FKCOLUMN_NAME");
               String pkTableName = keyMd.getString("PKTABLE_NAME");
               String pkColumnName = keyMd.getString("PKCOLUMN_NAME");

               Col fk = db.getCol(fkTableName, fkColumnName);
               Col pk = db.getCol(pkTableName, pkColumnName);
               fk.setPk(pk);

               System.out.println(fkTableName + "." + fkColumnName + " -> " + pkTableName + "." + pkColumnName);
            }
            keyMd.close();
         }
         rs.close();

         //-- if a table has two columns and both are foreign keys
         //-- then it is a relationship table for MANY_TO_MANY relationships
         for (Tbl table : db.getTbls())
         {
            List<Col> cols = table.getCols();
            if (cols.size() == 2 && cols.get(0).isFk() && cols.get(1).isFk())
            {
               table.setLinkTbl(true);
            }
         }
      }
      finally
      {
         Sql.close(apiConn);
      }
   }

   public void bootstrapApi(Api api, Db db) throws Exception
   {
      for (Tbl t : db.getTbls())
      {
         List<Col> cols = t.getCols();

         Collection collection = new Collection();
         String collectionName = t.getName();

         collectionName = Character.toLowerCase(collectionName.charAt(0)) + collectionName.substring(1, collectionName.length());

         if (!collectionName.endsWith("s"))
            collectionName = English.plural(collectionName);

         collection.setName(collectionName);

         Entity entity = new Entity();
         entity.setTbl(t);
         entity.setHint(t.getName());

         entity.setCollection(collection);
         collection.setEntity(entity);

         for (Col col : cols)
         {
            if (col.getPk() == null)
            {
               Attribute attr = new Attribute();
               attr.setEntity(entity);
               attr.setName(col.getName());
               attr.setCol(col);
               attr.setHint(col.getTbl().getName() + "." + col.getName());
               attr.setType(col.getType());

               entity.addAttribute(attr);

               if (col.isUnique() && entity.getKey() == null)
               {
                  entity.setKey(attr);
               }
            }
         }

         api.addCollection(collection);
         collection.setApi(api);

      }

      //-- Now go back through and create relationships for all foreign keys
      //-- two relationships objects are created for every relationship type
      //-- representing both sides of the relationship...ONE_TO_MANY also
      //-- creates a MANY_TO_ONE and there are always two for a MANY_TO_MANY.
      //-- API designers may want to represent one or both directions of the
      //-- relationship in their API and/or the names of the JSON properties
      //-- for the relationships will probably be different
      for (Tbl t : db.getTbls())
      {
         if (t.isLinkTbl())
         {
            Col fkCol1 = t.getCols().get(0);
            Col fkCol2 = t.getCols().get(1);

            //MANY_TO_MANY one way
            {
               Entity pkEntity = api.getEntity(fkCol1.getPk().getTbl());
               Relationship r = new Relationship();
               pkEntity.addRelationship(r);
               r.setEntity(pkEntity);
               r.setRelated(api.getEntity(fkCol2.getTbl()));

               String hint = "MANY_TO_MANY - ";
               hint += fkCol1.getPk().getTbl().getName() + "." + fkCol1.getPk().getName();
               hint += " <- " + fkCol1.getTbl().getName() + "." + fkCol1.getName() + ":" + fkCol2.getName();
               hint += " -> " + fkCol2.getPk().getTbl().getName() + "." + fkCol2.getPk().getName();

               r.setHint(hint);
               r.setType(r.REL_MANY_TO_MANY);
               r.setFkCol1(fkCol1);
               r.setFkCol2(fkCol2);

               //Collection related = api.getCollection(fkCol2.getTbl());
               r.setName(makeRelationshipName(r));
            }

            //MANY_TO_MANY the other way
            {
               Entity pkEntity = api.getEntity(fkCol2.getPk().getTbl());
               Relationship r = new Relationship();
               pkEntity.addRelationship(r);
               r.setEntity(pkEntity);
               r.setRelated(api.getEntity(fkCol1.getTbl()));

               String hint = "MANY_TO_MANY - ";
               hint += fkCol2.getPk().getTbl().getName() + "." + fkCol2.getPk().getName();
               hint += " <- " + fkCol2.getTbl().getName() + "." + fkCol2.getName() + ":" + fkCol1.getName();
               hint += " -> " + fkCol1.getPk().getTbl().getName() + "." + fkCol1.getPk().getName();

               r.setHint(hint);
               r.setType(r.REL_MANY_TO_MANY);
               r.setFkCol1(fkCol2);
               r.setFkCol2(fkCol1);

               r.setName(makeRelationshipName(r));
            }
         }
         else
         {
            for (Col col : t.getCols())
            {
               if (col.isFk())
               {
                  Col fkCol = col;
                  Tbl fkTbl = fkCol.getTbl();
                  Entity fkEntity = api.getEntity(fkTbl);

                  Col pkCol = col.getPk();
                  Tbl pkTbl = pkCol.getTbl();
                  Entity pkEntity = api.getEntity(pkTbl);

                  //ONE_TO_MANY
                  {
                     Relationship r = new Relationship();

                     //TODO:this name may not be specific enough or certain types
                     //of relationships. For example where an entity is related
                     //to another entity twice
                     r.setHint("MANY_TO_ONE - " + pkTbl.getName() + "." + pkCol.getName() + " <- " + fkTbl.getName() + "." + fkCol.getName());
                     r.setType(r.REL_MANY_TO_ONE);
                     r.setFkCol1(fkCol);
                     r.setEntity(pkEntity);
                     r.setRelated(fkEntity);
                     r.setName(makeRelationshipName(r));
                     pkEntity.addRelationship(r);
                  }

                  //MANY_TO_ONE
                  {
                     Relationship r = new Relationship();
                     r.setHint("ONE_TO_MANY - " + fkTbl.getName() + "." + fkCol.getName() + " -> " + pkTbl.getName() + "." + pkCol.getName());
                     r.setType(r.REL_ONE_TO_MANY);
                     r.setFkCol1(fkCol);
                     r.setEntity(fkEntity);
                     r.setRelated(pkEntity);
                     r.setName(makeRelationshipName(r));
                     fkEntity.addRelationship(r);
                  }
               }
            }
         }
      }
   }

   static String makeRelationshipName(Relationship rel)
   {
      String name = null;
      String type = rel.getType();
      if (type.equals(rel.REL_ONE_TO_MANY))
      {
         name = rel.getFkCol1().getName();
         if (name.toLowerCase().endsWith("id") && name.length() > 2)
         {
            name = name.substring(0, name.length() - 2);
         }
      }
      else if (type.equals(rel.REL_MANY_TO_ONE))
      {
         name = rel.getRelated().getCollection().getName();//.getTbl().getName();
         if (!name.endsWith("s"))
            name = English.plural(name);
      }
      else if (type.equals(rel.REL_MANY_TO_MANY))
      {
         name = rel.getFkCol2().getPk().getTbl().getName();
         if (!name.endsWith("s"))
            name = English.plural(name);
      }

      name = Character.toLowerCase(name.charAt(0)) + name.substring(1, name.length());

      return name;
   }

}
