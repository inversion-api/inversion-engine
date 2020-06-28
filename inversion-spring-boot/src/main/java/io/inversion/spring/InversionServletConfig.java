package io.inversion.spring;

import java.util.ArrayList;
import java.util.List;

import org.apache.catalina.Context;
import org.apache.catalina.core.StandardContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.inversion.Engine;
import io.inversion.EngineServlet;
import io.inversion.utils.Path;

@Configuration
public class InversionServletConfig {

   @Bean
   public ServletRegistrationBean inversionServlet(@Autowired Engine engine) {
      return createDefaultInversionServlet(engine);
   }

   public static ServletRegistrationBean createDefaultInversionServlet(Engine engine) {
      EngineServlet servlet = new EngineServlet();
      servlet.setEngine(engine);

      String servletMapping = buildServletMapping(engine);
      ServletRegistrationBean bean = new ServletRegistrationBean(servlet, servletMapping);

      bean.setLoadOnStartup(1);
      return bean;
   }

   /**
    * acumulates all of the static path parts
    * shared by all Engine includesPaths.
    */
   public static String buildServletMapping(Engine engine) {
      List parts = new ArrayList();
      boolean done = false;
      for (int i = 0; i < 100 && !done; i++) {
         String part = null;
         for (Path path : engine.getIncludePaths()) {
            if (part == null)
               part = path.get(i);

            if (part == null || !path.isStatic(i) || !part.equals(path.get(i))) {
               done = true;
               break;
            }
         }

         if (part == null)
            break;

         if (!done)
            parts.add(part);
      }
      return (parts.size() > 0 ? ("/" + new Path(parts)) : "") + "/*";
   }

   @Bean
   public ConfigurableServletWebServerFactory servletContainer() {
      return createDefaultServletContainer();
   }

   public static ConfigurableServletWebServerFactory createDefaultServletContainer() {
      TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
      tomcat.addContextCustomizers(new TomcatContextCustomizer() {

         @Override
         public void customize(Context context) {
            ((StandardContext) context).setAllowCasualMultipartParsing(true);
         }
      });

      return tomcat;
   }

}
