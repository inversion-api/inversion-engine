package io.inversion.spring;

import io.inversion.Engine;
import io.inversion.EngineServlet;
import io.inversion.utils.Path;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class InversionServletConfig {
    public static ServletRegistrationBean createDefaultInversionServlet(Engine engine) {
        EngineServlet servlet = new EngineServlet();
        servlet.setEngine(engine);

        String                                 servletMapping = buildServletMapping(engine);
        ServletRegistrationBean<EngineServlet> bean           = new ServletRegistrationBean<>(servlet, servletMapping);

        bean.setLoadOnStartup(1);
        return bean;
    }

    /**
     * @param engine the Engine hosting the Apis
     * @return accumulation all of the static path parts shared by all Engine includesPaths.
     */
    public static String buildServletMapping(Engine engine) {
        List<String> parts = new ArrayList<>();
        boolean      done  = false;
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

    public static ConfigurableServletWebServerFactory createDefaultServletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        tomcat.addContextCustomizers(context -> context.setAllowCasualMultipartParsing(true));

        tomcat.addConnectorCustomizers(connector -> {
            AbstractHttp11Protocol httpProtocol = (AbstractHttp11Protocol) connector.getProtocolHandler();
            httpProtocol.setCompressibleMimeType("text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json");
            httpProtocol.setCompression("1024");//compresses responses over 1KB.
        });


        return tomcat;
    }

    @Bean
    public ServletRegistrationBean inversionServlet(@Autowired Engine engine) {
        return createDefaultInversionServlet(engine);
    }

    @Bean
    public ConfigurableServletWebServerFactory servletContainer() {
        return createDefaultServletContainer();
    }

}
