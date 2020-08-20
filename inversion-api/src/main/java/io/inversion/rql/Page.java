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
package io.inversion.rql;

/**
 * page(pageNum, [pageSize])
 * pageNum(pageNum, [pageSize])
 * offset(offset, [limit])
 * limit(limit, [offset])
 * pageSize(pageSize)
 *
 * @param <T> the subclass of Page
 * @param <P> the subclass of Query
 */
public class Page<T extends Page, P extends Query> extends Builder<T, P> {
    public static final int DEFAULT_LIMIT = 100;

    public Page(P query) {
        super(query);
        withFunctions("offset", "limit", "page", "pageNum", "pageSize", "after");
    }

    public int getOffset() {
        int offset = findInt("offset", 0, -1);

        if (offset < 0)
            offset = findInt("limit", 1, -1);

        if (offset < 0) {
            int limit = getLimit();
            if (limit > 0) {
                int page = findInt("page", 0, -1);
                if (page < 0)
                    page = findInt("pageNum", 0, -1);

                if (page >= 0) {
                    offset = Math.max(0, (page - 1)) * limit;
                }
            }
        }

        if (offset < 0)
            offset = 0;

        return offset;
    }

    public Term getAfter() {
        return find("after");
    }

    public int getLimit() {
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

    public int getPageSize() {
        return getLimit();
    }

    public int getPage() {
        int page   = -1;
        int offset = getOffset();
        if (offset > -1) {
            int limit = getLimit();
            if (limit > -1) {
                page = (offset / limit) + 1;
            }
        }
        return page < 1 ? 1 : page;
    }

    public int getPageNum() {
        return getPage();
    }

}
