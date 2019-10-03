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
package io.inversion.cloud.utils;

import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.utils.HttpUtils.FutureResponse;

public class Http
{
   public FutureResponse get(String url)
   {
      return HttpUtils.get(url);
   }

   public FutureResponse delete(String url)
   {
      return HttpUtils.delete(url);
   }

   public FutureResponse post(String url, JSNode body)
   {
      return HttpUtils.post(url, body.toString());
   }

   public FutureResponse put(String url, JSNode body)
   {
      return HttpUtils.put(url, body.toString());
   }

   public FutureResponse request(String method, String url, JSNode body)
   {
      return HttpUtils.rest(method, url, body.toString(), null, 0);
   }

}
