package io.rocketpartners.cloud.service.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.service.Servlet;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class SpringBoot
{
   static Service service = null;

   public static void run(Service inService)
   {
      service = inService;
      SpringApplication.run(SpringBoot.class);
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
