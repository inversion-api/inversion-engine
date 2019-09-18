package io.rocketpartners.cloud.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.service.Servlet;
import io.rocketpartners.cloud.utils.Utils;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class Inversion
{
   static Service service = null;

   public static void main(String[] args)
   {
      run(new Service());
   }

   /**
    * Convenience method for launching a Service with a single API.
    * @param api
    */
   public static void run(Api api)
   {
      run(new Service().withApi(api));
   }

   public static void run(Service inService)
   {
      try
      {
         service = inService;
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
            msg += "\n SOLUTION: Override Service.startup0() and place all of your Api wiring";
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
         Servlet servlet = new io.rocketpartners.cloud.service.Servlet();
         servlet.setService(service);

         String servletMapping = service.getServletMapping();
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
