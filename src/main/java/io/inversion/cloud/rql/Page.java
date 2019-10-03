/*
 * Copyright (c) 2015-2019 Inversion.org, LLC
 * https://github.com/inversion-api
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
package io.inversion.cloud.rql;

/**
 * 
 * page(pageNum, [pageSize])
 * pageNum(pageNum, [pageSize])
 * offset(offset, [limit])
 * limit(limit, [offset])
 * pageSize(pageSize)
 * 
 * 
 * @author wells
 *
 * @param <T>
 * @param <P>
 */
public class Page<T extends Page, P extends Query> extends Builder<T, P>
{
   public static final int DEFAULT_LIMIT = 100;

   public Page(P query)
   {
      super(query);
      withFunctions("offset", "limit", "page", "pageNum", "pageSize", "after");
   }

   public int getOffset()
   {
      int offset = findInt("offset", 0, -1);

      if (offset < 0)
         offset = findInt("limit", 1, -1);

      if (offset < 0)
      {
         int limit = getLimit();
         if (limit > 0)
         {
            int page = findInt("page", 0, -1);
            if(page < 0)
               page = findInt("pageNum", 0, -1);
            
            if (page >= 0)
            {
               offset = Math.max(0, (page - 1)) * limit;
            }
         }
      }

      if(offset < 0) 
         offset = 0;
      
      return offset;
   }

   public Term getAfter()
   {
      return find("after");
   }
   
   public int getLimit()
   {
      int limit = findInt("limit", 0, -1);

      if (limit < 0)
         limit = findInt("offset", 1, -1);

      if (limit < 0)
         limit = findInt("pageSize", 0, -1);

      if (limit < 0)
         limit = findInt("page", 1, -1);

      if (limit < 0)
         limit = DEFAULT_LIMIT;

      return limit;
   }

   public int getPageSize()
   {
      return getLimit();
   }

   public int getPage()
   {
      int page = -1;
      int offset = getOffset();
      if (offset > -1)
      {
         int limit = getLimit();
         if (limit > -1)
         {
            page = (offset / limit) + 1;
         }
      }
      return page < 1 ? 1 : page;
   }

   public int getPageNum()
   {
      return getPage();
   }

   public T offset(int offset)
   {
      withTerm("offset", offset);
      return r();
   }

   public T limit(int limit)
   {
      withTerm("limit", limit);
      return r();
   }

   public T page(int page)
   {
      withTerm("page", page);
      return r();
   }

   public T pageNum(int page)
   {
      withTerm("pageNum", page);
      return r();
   }

   public T pageSize(int pageSize)
   {
      withTerm("pageSize", pageSize);
      return r();
   }

}
