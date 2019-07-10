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
 * //
 */
package io.rocketpartners.cloud.service.spring;

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.service.Servlet;

/**
 * @author kfrankic
 *
 */
@RestController
public class Controller implements InitializingBean
{
   protected Logger         log     = LoggerFactory.getLogger(Controller.class);

   @Autowired
   protected ResourceLoader resourceLoader;

   @Autowired
   protected Environment    environment;

   @Autowired(required = false)
   protected Service        service = null;

   protected Servlet        servlet = new Servlet();

   /**
    * Subclasses can override if it is easier to directly wire up 
    * a service/api in this code here verses other options.
    * 
    * By default this will return the service that was autowired
    * by spring if it exists.
    * 
    * If no service is autowired or created by a subclass here
    * then afterPropertiesSet() will create a default one.  In 
    * that case, you must have config files that will be discovered
    * by the configuration process and used to build the
    * api.
    * 
    * @return
    */
   public Service getService()
   {
      return service;
   }

   @Override
   public void afterPropertiesSet() throws Exception
   {
      try
      {
         if(service == null)
            service = new Service();
         
         servlet.setService(service);

         String[] activeProfiles = environment.getActiveProfiles();
         String profile = null;
         if (activeProfiles.length > 0)
         {
            profile = activeProfiles[0];
            log.info("Using profile '" + profile + "'");
         }
         else
         {
            log.info("No active spring profile was configured - use 'spring.profiles.active' to configure one");
         }

         final ResourceLoaderServletContext ctx = new ResourceLoaderServletContext(resourceLoader);

         servlet.getService().setResourceLoader(new Service.ResourceLoader()
            {
               @Override
               public InputStream getResource(String name)
               {
                  return ctx.getResourceAsStream(name);
               }
            });

         servlet.getService().setProfile(profile);
         servlet.getService().startup();

      }
      catch (Exception e)
      {
         e.printStackTrace();

         log.error("Error initializing.", e);
         throw e;
      }
   }

   @RequestMapping(value = "/**")
   public void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws Exception
   {
      servlet.service(req, resp);
   }

   static class ResourceLoaderServletContext extends MockServletContext
   {

      public ResourceLoaderServletContext(ResourceLoader resourceLoader)
      {
         super(resourceLoader);
      }

      @Override
      protected String getResourceLocation(String path)
      {
         if (path != null)
         {
            path = path.replace("/WEB-INF/", "classpath:");
            path = path.replace("WEB-INF/", "classpath:");

            if (!path.startsWith("classpath:"))
            {
               path = "classpath:" + path;
            }

         }
         return path;
      }

   }

}
