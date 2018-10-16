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
package io.rcktapp.api;

import java.util.ArrayList;
import java.util.List;

public class Db extends Dto
{
   long             orgId         = 0;
   String           name          = null;
   String           type          = null;
   String           driver        = null;
   String           url           = null;
   String           user          = null;
   String           pass          = null;
   int              poolMin       = 3;
   int              poolMax       = 10;
   ArrayList<Table> tables        = new ArrayList();

   // set this to false, if you don't want to Snooze.bootstrapDb to do anything
   boolean          bootstrap     = true;

   // set this to false to turn off SQL_CALC_FOUND_ROWS and SELECT FOUND_ROWS()
   boolean          calcRowsFound = true;

   public Db()
   {
   }

   public Db(String name)
   {
      this.name = name;
   }

   public Table getTable(String tableName)
   {
      for (Table t : tables)
      {
         if (t.getName().equalsIgnoreCase(tableName))
            return t;
      }
      return null;
   }

   public Column getColumn(String table, String col)
   {
      for (Table t : tables)
      {
         if (t.getName().equalsIgnoreCase(table))
         {
            for (Column c : t.getColumns())
            {
               if (c.getName().equalsIgnoreCase(col))
               {
                  return c;
               }
            }

            return null;
         }
      }
      return null;
   }

   public String getQuote()
   {
      return "'";
   }

   /**
    * @return the tables
    */
   public List<Table> getTables()
   {
      return tables;
   }

   /**
    * @param tables the tables to set
    */
   public void setTables(List<Table> tbls)
   {
      this.tables = new ArrayList(tbls);
   }

   public void addTable(Table tbl)
   {
      if (tbl != null && !tables.contains(tbl))
      {
         tables.add(tbl);
      }
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
   public void setName(String name)
   {
      this.name = name;
   }

   /**
    * @return the driver
    */
   public String getDriver()
   {
      return driver;
   }

   /**
    * @param driver the driver to set
    */
   public void setDriver(String driver)
   {
      this.driver = driver;
   }

   /**
    * @return the url
    */
   public String getUrl()
   {
      return url;
   }

   /**
    * @param url the url to set
    */
   public void setUrl(String url)
   {
      this.url = url;
   }

   /**
    * @return the username
    */
   public String getUser()
   {
      return user;
   }

   /**
    * @param username the username to set
    */
   public void setUser(String user)
   {
      this.user = user;
   }

   /**
    * @return the password
    */
   public String getPass()
   {
      return pass;
   }

   /**
    * @param password the password to set
    */
   public void setPass(String password)
   {
      this.pass = password;
   }

   public long getAccountId()
   {
      return orgId;
   }

   public void setOrgId(long orgId)
   {
      this.orgId = orgId;
   }

   public int getPoolMin()
   {
      return poolMin;
   }

   public void setPoolMin(int poolMin)
   {
      this.poolMin = poolMin;
   }

   public int getPoolMax()
   {
      return poolMax;
   }

   public void setPoolMax(int poolMax)
   {
      this.poolMax = poolMax;
   }

   public boolean isBootstrap()
   {
      return bootstrap;
   }

   public void setBootstrap(boolean bootstrap)
   {
      this.bootstrap = bootstrap;
   }

   public boolean isCalcRowsFound()
   {
      return calcRowsFound;
   }

   public void setCalcRowsFound(boolean calcRowsFound)
   {
      this.calcRowsFound = calcRowsFound;
   }

}
