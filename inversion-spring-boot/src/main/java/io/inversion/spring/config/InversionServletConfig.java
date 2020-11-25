/*
 * Copyright (c) 2015-2020 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.spring.config;

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

    public InversionServletConfig()
    {
        System.out.println("InversionServletConfig()<>");
    }


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
            for (Path path : engine.getAllIncludePaths()) {
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
        //System.out.println("ServletRegistrationBean.inversionServlet(engine" + System.identityHashCode(engine) + ")");
        return createDefaultInversionServlet(engine);
    }

    @Bean
    public ConfigurableServletWebServerFactory servletContainer() {
        //System.out.println("ConfigurableServletWebServerFactory.servletContainer()");
        return createDefaultServletContainer();
    }

}
