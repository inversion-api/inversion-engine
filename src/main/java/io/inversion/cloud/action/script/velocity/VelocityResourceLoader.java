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
package io.inversion.cloud.action.script.velocity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringBufferInputStream;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.apache.velocity.util.ExtProperties;

import io.inversion.cloud.action.script.ScriptAction;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.service.Engine;

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
