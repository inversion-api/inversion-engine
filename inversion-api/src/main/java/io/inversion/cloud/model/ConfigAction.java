/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.inversion.cloud.utils.Utils;

/**
 * @author wells
 */
public class ConfigAction extends Action<ConfigAction>
{
   public ConfigAction()
   {

   }

   public ConfigAction(String includePaths, String excludePaths, String config)
   {
      super(includePaths, excludePaths, config);
   }

   public final void run(Request req, Response res) throws Exception
   {
      //does nothing on purpose!
   }

}
