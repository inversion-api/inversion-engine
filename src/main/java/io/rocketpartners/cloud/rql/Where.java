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
package io.rocketpartners.cloud.rql;

import java.util.List;

public class Where<T extends Where, P extends Query> extends Builder<T, P>
{

   public Where(P query)
   {
      super(query);
      withTokens("and", "or", "not", "eq", "ne", "n", "nn", "like", "w", "sw", "ew", "lt", "le", "gt", "ge", "in", "out", "if", "w", "wo", "emp", "nemp");
   }

   public List<Term> filters()
   {
      return getTerms();
   }

   public T and(Object... terms)
   {
      return withTerm("and", terms);
   }

   public T or(Object... terms)
   {
      return withTerm("or", terms);
   }

   public T not(Object... terms)
   {
      return withTerm("not", terms);
   }

   public T eq(Object... terms)
   {
      return withTerm("eq", terms);
   }

   public T ne(Object... terms)
   {
      return withTerm("ne", terms);
   }

   public T like(Object... terms)
   {
      return withTerm("like", terms);
   }

   public T lt(Object... terms)
   {
      return withTerm("lt", terms);
   }

   public T gt(Object... terms)
   {
      return withTerm("eq", terms);
   }

   public T in(Object... terms)
   {
      return withTerm("in", terms);
   }

   public T out(Object... terms)
   {
      return withTerm("out", terms);
   }

   public T iff(Object... terms)
   {
      return withTerm("if", terms);
   }

   public T w(Object... terms)
   {
      return withTerm("w", terms);
   }

   public T wo(Object... terms)
   {
      return withTerm("wo", terms);
   }

   public T emp(Object... terms)
   {
      return withTerm("emp", terms);
   }

   public T nemp(Object... terms)
   {
      return withTerm("nemp", terms);
   }

}
