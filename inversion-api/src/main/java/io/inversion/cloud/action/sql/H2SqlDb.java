/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.action.sql;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.h2.tools.Server;

import io.inversion.cloud.utils.SqlUtils;
import io.inversion.cloud.utils.Utils;

/**
 * This is a convenience class that extends SqlDb with the ability to 
 * delete an existing H2 db and then bootstrap it with sql statements.
 * <b>
 * This is super useful for testing where you want the DB to be in the
 * same state each time you rerun your tests.
 * 
 * @author wells
 *
 */
public class H2SqlDb extends SqlDb
{
   static Object LOCK    = new Object();
   static Server server  = null;

   String        h2Dir   = new File(System.getProperty("user.home"), ".inversion").toString();
   String        h2File;
   List<String>  ddlUrls = new ArrayList();
   boolean       resetDb = true;

   public H2SqlDb()
   {
      this(null, null, null, null);
   }

   public H2SqlDb(String name, String h2File, String... ddlUrl)
   {
      withName(name);
      withType("h2");
      withDriver("org.h2.Driver");
      withUser("sa");
      withPass("");
      withH2File(h2File);
      withDdlUrl(ddlUrl);
   }

   @Override
   protected DataSource createConnectionPool()
   {
      try
      {
         synchronized (LOCK)
         {
            if (server == null)
            {
               server = Server.createTcpServer("-ifNotExists");
               server.start();
            }
         }

         if (isResetDb())
         {
            File dir = new File(getH2Dir());
            File[] files = dir.listFiles();
            for (int i = 0; files != null && i < files.length; i++)
            {
               if (files[i].getName().startsWith(getH2File()))
               {
                  System.out.println("Deleting old h2 file: " + files[i]);
                  files[i].delete();
               }
            }
         }

         File f = new File(getH2Dir(), h2File + ".mv.db");
         if (!f.exists())
         {
            if (ddlUrls.size() > 0)
            {

               Class.forName(getDriver());
               Connection conn = DriverManager.getConnection(getUrl(), getUser(), getPass());

               try
               {
                  for (String ddlUrl : ddlUrls)
                  {
                     SqlUtils.runDdl(conn, new URL(ddlUrl).openStream());
                  }
                  conn.commit();
               }
               finally
               {
                  conn.close();
               }
            }
         }

         return super.createConnectionPool();
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
         Utils.rethrow(ex);
      }
      return null;
   }

   public H2SqlDb withH2Dir(String h2Dir)
   {
      this.h2Dir = h2Dir;
      return this;
   }

   public H2SqlDb withH2File(String h2File)
   {
      this.h2File = h2File;
      return this;
   }

   public H2SqlDb withResetDb(boolean resetDb)
   {
      this.resetDb = resetDb;
      return this;
   }

   public H2SqlDb withDdlUrl(String... ddlUrl)
   {
      for (int i = 0; ddlUrl != null && i < ddlUrl.length; i++)
      {
         ddlUrls.add(ddlUrl[i]);
      }

      return this;
   }

   @Override
   public String getUrl()
   {
      String url = super.getUrl();
      if (url == null && getH2Dir() != null && h2File != null)
      {
         File file = new File(getH2Dir(), getH2File());
         if(!file.getParentFile().exists())
            file.getParentFile().mkdirs();
         
         url = "jdbc:h2:tcp://localhost:9092/nio:" + getH2Dir() + "/" + getH2File() + ";AUTO_SERVER=TRUE;IFEXISTS=false";
      }
      return url;
   }

   public String getH2Dir()
   {
      return h2Dir;
   }

   public String getH2File()
   {
      return h2File;
   }

   public List<String> getDdlUrls()
   {
      return new ArrayList(ddlUrls);
   }

   public boolean isResetDb()
   {
      return resetDb;
   }

}
