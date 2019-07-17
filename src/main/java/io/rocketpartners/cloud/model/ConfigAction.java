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
package io.rocketpartners.cloud.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.rocketpartners.cloud.service.Chain;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Utils;

/**
 * @author wells
 */
public class ConfigAction extends Action<ConfigAction>
{
   public ConfigAction()
   {

   }

   public ConfigAction(String config)
   {
      super(config);
   }

   public ConfigAction(String config, String includePaths, String excludePaths)
   {
      super(config, includePaths, excludePaths);
   }

   public final void run(Service service, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      //does nothing on purpose!
   }

  

}
