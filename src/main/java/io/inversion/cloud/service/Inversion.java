/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
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
package io.inversion.cloud.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

import io.inversion.cloud.model.Api;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.service.Servlet;
import io.inversion.cloud.utils.Utils;

/**
 * A simple Spring Boot based launcher.
 */
@SpringBootApplication
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class Inversion
{
   static Engine engine = null;

   public static void main(String[] args)
   {
      run(new Engine());
   }

   /**
    * Convenience method for launching a Engine with a single API.
    * @param api
    */
   public static void run(Api api)
   {
      run(new Engine().withApi(api));
   }

   public static void run(Engine engine)
   {
      try
      {
         Inversion.engine = engine;
         SpringApplication.run(Inversion.class);
      }
      catch (Throwable e)
      {
         e = Utils.getCause(e);
         if (Utils.getStackTraceString(e).indexOf("A child container failed during start") > -1)
         {
            String msg = "";
            msg = " README FOR HELP!!!!!!!";
            msg += "\n";
            msg += "\n It looks like you are getting a frustrating Tomcat startup error.";
            msg += "\n";
            msg += "\n This error may be casused if URL.setURLStreamHandlerFactory()";
            msg += "\n is somehow called before Spring Boot starts Tomcat. ";
            msg += "\n";
            msg += "\n This seems to be a frustrating undocumented \"no no\" of Tomcat with ";
            msg += "\n Spring Boot. Using H2 db before Spring Boot starts Tomcat seems to ";
            msg += "\n be one known cause of this error.";
            msg += "\n";
            msg += "\n SOLUTION: Override Engine.startup0() and place all of your Api wiring";
            msg += "\n and other setup code there.  That way Tomcat will load before ";
            msg += "\n the part of your code that is causing this unintended side effect.";
            msg += "\n\n\n";

            System.err.println(msg);
            throw new RuntimeException(msg, e);
         }
         Utils.rethrow(e);
      }
   }

   @Bean
   public ServletRegistrationBean inversionServlet()
   {
      try
      {
         Servlet servlet = new io.inversion.cloud.service.Servlet();
         servlet.setEngine(engine);

         String servletMapping = engine.getServletMapping() != null ? engine.getServletMapping().toString() : null;
         if (servletMapping == null)
            servletMapping = "/*";

         if (!servletMapping.endsWith("*"))
         {
            if (!servletMapping.endsWith("/"))
               servletMapping += "/";

            servletMapping += "*";
         }

         if (!servletMapping.startsWith("/"))
            servletMapping = "/" + servletMapping;

         ServletRegistrationBean bean = new ServletRegistrationBean(servlet, servletMapping);
         bean.setLoadOnStartup(1);
         return bean;
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
         throw new RuntimeException(ex);
      }

   }

}
