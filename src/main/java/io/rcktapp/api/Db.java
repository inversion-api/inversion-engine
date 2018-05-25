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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.rcktapp.api;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class Db extends Dto
{
   long                         orgId       = 0;
   String                       name        = null;
   String                       driver      = null;
   String                       url         = null;
   String                       user        = null;
   String                       pass        = null;
   int                          poolMin     = 3;
   int                          poolMax     = 10;
   ArrayList<Tbl>               tbls        = new ArrayList();

   static Map<Long, DataSource> dataSources = new Hashtable();

   public Db()
   {
   }

   public Db(String name)
   {
      this.name = name;
   }

   public Tbl getTbl(String tableName)
   {
      for (Tbl t : tbls)
      {
         if (t.getName().equalsIgnoreCase(tableName))
            return t;
      }
      return null;
   }

   public Col getCol(String table, String col)
   {
      for (Tbl t : tbls)
      {
         if (t.getName().equalsIgnoreCase(table))
         {
            for (Col c : t.getCols())
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

   /**
    * @return the tables
    */
   public List<Tbl> getTbls()
   {
      return tbls;
   }

   /**
    * @param tables the tables to set
    */
   public void setTbls(List<Tbl> tbls)
   {
      this.tbls = new ArrayList(tbls);
   }

   public void addTbl(Tbl tbl)
   {
      if (tbl != null && !tbls.contains(tbl))
      {
         tbls.add(tbl);
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

   public void setTbls(ArrayList<Tbl> tbls)
   {
      this.tbls = tbls;
   }

   public long getOrgId()
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

   public DataSource getDs() throws Exception
   {
      DataSource ds = dataSources.get(getId());
      if (ds == null && !dataSources.containsKey(getId()))
      {
         synchronized (this)
         {
            try
            {
               ds = dataSources.get(getId());

               if (ds == null)
               {
                  ComboPooledDataSource cpds = new ComboPooledDataSource();

                  cpds.setDriverClass(getDriver());
                  cpds.setJdbcUrl(getUrl());
                  cpds.setUser(getUser());
                  cpds.setPassword(getPass());
                  cpds.setMinPoolSize(getPoolMin());
                  cpds.setMaxPoolSize(getPoolMax());

                  ds = cpds;

               }
            }
            finally
            {
               //do this even when ther is an error or the system will 
               //forever try to recreate the datasource
               dataSources.put(getId(), ds);
            }
         }
      }
      return ds;

   }

}
