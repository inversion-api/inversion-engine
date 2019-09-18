/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
package io.rocketpartners.cloud.action.sql;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;

import javax.sql.DataSource;

import org.h2.tools.Server;

import io.rocketpartners.cloud.utils.SqlUtils;
import io.rocketpartners.cloud.utils.Utils;

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
   static Object LOCK      = new Object();
   static Server server    = null;

   String        h2Dir     = new File(System.getProperty("user.home"), ".inversion").toString();
   String        h2File    = null;
   InputStream   ddlStream = null;
   boolean       resetDb   = true;

   public H2SqlDb(String name, String h2File, boolean resetDb, InputStream ddlStream)
   {
      this(name, null, h2File, resetDb, ddlStream);
   }

   public H2SqlDb(String name, String h2Dir, String h2File, boolean resetDb, InputStream ddlStream)
   {
      withName(name);
      withDriver("org.h2.Driver");
      withUser("sa");
      withPass("");
      if (h2Dir != null)
         withH2Dir(h2Dir);
      withH2File(h2File);
      withResetDb(resetDb);
      withDdlStream(ddlStream);
   }

   public DataSource createConnectionPool()
   {
      try
      {
         synchronized (LOCK)
         {
            if (server == null)
            {
               server = Server.createTcpServer();
               server.start();
            }
         }

         String url = "jdbc:h2:tcp://localhost:9092/nio:" + h2Dir + "/" + h2File + ";AUTO_SERVER=TRUE";
         withUrl(url);

         if (resetDb)
         {
            File dir = new File(h2Dir);
            File[] files = dir.listFiles();
            for (int i = 0; files != null && i < files.length; i++)
            {
               if (files[i].getName().startsWith(h2File))
               {
                  System.out.println("Deleting old h2 file: " + files[i]);
                  files[i].delete();
               }
            }
         }

         //Server.createTcpServer("-tcpPort", port + "").start();

         if (ddlStream != null)
         {
            File f = new File(h2Dir, h2File + ".mv.db");
            if (!f.exists())
            {
               Class.forName(getDriver());
               Connection conn = DriverManager.getConnection(url, getUser(), getPass());
               SqlUtils.runDdl(conn, ddlStream);
               conn.commit();
               conn.close();
            }
         }

         return super.createConnectionPool();
      }
      catch (Exception ex)
      {
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

   public H2SqlDb withDdlStream(InputStream ddlStream)
   {
      this.ddlStream = ddlStream;
      return this;
   }

}
