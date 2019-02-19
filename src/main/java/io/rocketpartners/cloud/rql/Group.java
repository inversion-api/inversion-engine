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
package io.rocketpartners.cloud.rql;

import java.util.ArrayList;
import java.util.List;

public class Group<T extends Group, P extends Query> extends Builder<T, P>
{
   public Group(P query)
   {
      super(query);
      withFunctions("group");
   }

   public List<String> groups()
   {
      List<String> groups = new ArrayList();
      for (Term group : findAll("group"))
      {
         for (Term term : group.getTerms())
         {
            if (term.isLeaf())
               groups.add(term.getToken());
         }
      }
      return groups;
   }

   public T group(String... properties)
   {
      Term group = find("group");
      if (group != null)
      {
         for (String property : properties)
         {
            group.withTerm(Term.term(group, property));
         }
      }
      else
      {
         withTerm("group", (Object[]) properties);
      }

      return r();
   }
}
