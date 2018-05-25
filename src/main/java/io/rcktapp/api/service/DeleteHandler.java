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
package io.rcktapp.api.service;

import java.sql.Connection;

import io.forty11.sql.Sql;
import io.rcktapp.api.Chain;
import io.rcktapp.api.Collection;
import io.rcktapp.api.Entity;
import io.rcktapp.api.Request;
import io.rcktapp.api.Response;
import io.rcktapp.api.Rule;

public class DeleteHandler extends SqlHandler
{
   @Override
   public void service(Service service, Chain chain, Rule rule, Request req, Response res) throws Exception
   {
      Connection conn = null;
      try
      {
         conn = ((Snooze) service).getConnection(req.getApi(), req.getCollectionKey());
         Collection collection = req.getApi().getCollection(req.getCollectionKey());
         Entity entity = collection.getEntity();
         Object entityKey = req.getEntityKey();

         String table = entity.getTbl().getName();
         entityKey = filterId(rule, req, conn, table, entityKey);

         if (entityKey != null)
         {
            String sql = "DELETE FROM " + entity.getTbl().getName() + " WHERE id = " + Sql.check(entityKey);
            Sql.execute(conn, sql);
         }

         //         for (Relationship rel : entity.getRelationships())
         //         {
         //            if ("MANY_TO_MANY".equalsIgnoreCase(rel.getType()) || "MANY_TO_ONE".equalsIgnoreCase(rel.getType()))
         //            {
         //               String fkCol = rel.getCol().getName();
         //               String fkTbl = rel.getCol().getTbl().getName();
         //               boolean deleted = rel.getCol().getTbl().getCol("deleted") != null;
         //               if (deleted)
         //               {
         //                  String sql = "";
         //                  sql += " UPDATE " + fkTbl + " SET deleted = true WHERE " + fkCol + " = ?";
         //                  Dao.execute(conn, sql, entityKey);
         //               }
         //               else
         //               {
         //                  String sql = "";
         //                  sql += " DELETE FROM " + fkTbl + " WHERE " + fkCol + " = ?";
         //                  Dao.execute(conn, sql, entityKey);
         //               }
         //            }
         //            else if ("ONE_TO_MANY".equalsIgnoreCase(rel.getType()))
         //            {
         //               //nothing todo.  these will be taken care of
         //               //when the main entity is deleted
         //            }
         //         }
         //
         //         if (col.getTbl().getCol("deleted") != null)
         //         {
         //            String sql = "";
         //            sql += " UPDATE " + col.getTbl().getName() + " SET deleted = true WHERE id = ?";
         //            Dao.execute(conn, sql, entityKey);
         //         }
         //         else
         //         {
         //                     String sql = "";
         //                     sql += " DELETE FROM " + entity.getTbl().getName() + " WHERE id = ?";
         //                     Sql.execute(conn, sql, entityKey);
         //         }
      }
      finally
      {
         if (conn != null)
            conn.close();
      }
   }
}
