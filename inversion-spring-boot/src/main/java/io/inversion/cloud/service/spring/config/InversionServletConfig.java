package io.inversion.cloud.service.spring.config;

import io.inversion.cloud.service.Engine;
import io.inversion.cloud.service.Servlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InversionServletConfig
{
    @Bean
    public ServletRegistrationBean inversionServlet(@Autowired Engine engine)
    {
        Servlet servlet = new Servlet();
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
}
