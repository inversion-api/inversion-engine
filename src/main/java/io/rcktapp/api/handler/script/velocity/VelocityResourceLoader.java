package io.rcktapp.api.handler.script.velocity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringBufferInputStream;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.apache.velocity.util.ExtProperties;

import io.forty11.web.js.JSObject;
import io.rcktapp.api.handler.script.ScriptHandler;
import io.rcktapp.api.service.Service;

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
         JSObject script = ScriptHandler.findScript(source);
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
