/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
package io.inversion.cloud.action.script.velocity;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.apache.velocity.util.ExtProperties;

import io.inversion.cloud.action.script.ScriptAction;
import io.inversion.cloud.model.JSNode;

public class VelocityResourceLoader extends ResourceLoader
{
   @Override
   public void init(ExtProperties configuration)
   {
      // TODO Auto-generated method stub

   }

   @Override
   public Reader getResourceReader(String source, String encoding) throws ResourceNotFoundException
   {
      try
      {
         JSNode script = ScriptAction.findScript(source);
         if (script != null)
         {
            return new InputStreamReader(new ByteArrayInputStream(script.getString("script").getBytes()));
         }
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
      }

      return null;
   }

   @Override
   public boolean isSourceModified(Resource resource)
   {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public long getLastModified(Resource resource)
   {
      // TODO Auto-generated method stub
      return 0;
   }

}
