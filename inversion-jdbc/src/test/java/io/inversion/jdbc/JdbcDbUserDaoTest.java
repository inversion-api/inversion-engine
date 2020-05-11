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
package io.inversion.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.inversion.User;
import io.inversion.jdbc.JdbcDb;
import io.inversion.jdbc.JdbcDbUserDao;
import io.inversion.jdbc.JdbcUtils;
import io.inversion.utils.Rows;
import io.inversion.utils.Rows.Row;

@TestInstance(Lifecycle.PER_CLASS)
public class JdbcDbUserDaoTest
{
   JdbcDb        db      = null;
   JdbcDbUserDao userDao = null;

   @AfterAll
   public void afterAll()
   {
      db.shutdown();
   }

   @BeforeAll
   public void beforeAll()
   {
      db = new JdbcDb("JdbcDbUserDaoTest", //
                      "org.h2.Driver", //
                      "jdbc:h2:mem:JdbcDbUserDaoTest;IGNORECASE=TRUE;DB_CLOSE_DELAY=-1", //
                      "sa", //
                      "", //
                      JdbcDbUserDaoTest.class.getResource("users-h2.ddl").toString(), //
                      JdbcDbUserDaoTest.class.getResource("test-users-h2.ddl").toString())
         {
            protected void doShutdown()
            {
               try
               {
                  JdbcUtils.execute(getConnection(), "SHUTDOWN");
               }
               catch (Exception ex)
               {
                  ex.printStackTrace();
               }
               super.doShutdown();
            }
         };

      userDao = new JdbcDbUserDao()
         {
            @Override
            protected boolean checkPassword(String actual, String supplied)
            {
               return super.checkPassword(actual, supplied);
            }

         };
      userDao.withSalt("1tHbDUZ6RHXp0Xrgl59wo5mJEoCQbQm4");
      userDao.withDb(db);
   }

   @Test
   public void findGRP_userHasOnlyAssignedGroupsRolesAndPermissions() throws Exception
   {
      Rows grps = userDao.findGRP(db.getConnection(), 10, "someApi", null);
      assertTrue(findPermission(grps, "permission1", "user->permission"));
      assertTrue(findPermission(grps, "permission2", "user->permission"));
      assertTrue(findPermission(grps, "permission2", "user->group->permission"));
      assertTrue(findPermission(grps, "permission3", "user->group->permission"));
      assertTrue(findPermission(grps, "permission4", "user->group->role->permission"));
      assertTrue(findPermission(grps, "permission5", "user->role->permission"));

      User user = userDao.getUser(null, "api_admin", "password", "someApi", null);

      assertEquals(0, CollectionUtils.disjunction(Arrays.asList("Administrator", "Member"), user.getRoles()).size());
      assertEquals(0, CollectionUtils.disjunction(Arrays.asList("admin_users", "read_only"), user.getGroups()).size());
      assertEquals(0, CollectionUtils.disjunction(Arrays.asList("permission1", "permission2", "permission3", "permission4", "permission5"), user.getPermissions()).size());
   }

   boolean findPermission(Rows rows, String name, String via)
   {
      for (Row row : rows)
      {
         String type = row.getString("type");
         String n = row.getString("name");
         String v = row.getString("via");

         if (type.equals("permission"))
         {
            if (name.equals(n) && via.equals(via))
               return true;
         }
      }
      return false;
   }
}
