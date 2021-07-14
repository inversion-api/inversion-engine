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
import io.inversion.utils.Config;
import io.inversion.utils.Path;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import javax.servlet.MultipartConfigElement;
import javax.servlet.annotation.MultipartConfig;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class InversionServletConfig {

    public InversionServletConfig() {
        System.out.println("InversionServletConfig()<>");
    }

    static final long maxFileSize = 5 * 1024 * 1024;
    static final long maxRequestSize = 5 * 1024 * 1024;


//    @Bean
//    public MultipartConfigElement buildMultipartConfig() {
//        MultipartConfigFactory mcf = new MultipartConfigFactory();
//
//        //spring.servlet.multipart.max-file-size=1024
//        //spring.servlet.multipart.max-request-size=1024
//
//        DataSize maxFile = DataSize.parse(Config.getString("spring.servlet.multipart.max-file-size", maxFileSize + ""));
//        DataSize reqReq = DataSize.parse(Config.getString("spring.servlet.multipart.max-request-size", maxRequestSize + ""));
//
//        mcf.setMaxFileSize(maxFile);
//        mcf.setMaxRequestSize(reqReq);
//
//        MultipartConfigElement mce = mcf.createMultipartConfig();
//
//        System.out.println(mce.getMaxFileSize());
//        return mce;
//    }

//    @Bean
//    public CommonsMultipartResolver multipartResolver(){
//        CommonsMultipartResolver resolver = new CommonsMultipartResolver();
//        resolver.setMaxUploadSize(5242880); // set the size limit
//        return resolver;
//    }


    @Bean
    public ServletRegistrationBean buildEngineServlet(@Autowired Engine engine) {
        EngineServlet servlet = new EngineServlet();
        servlet.setEngine(engine);

        String                                 servletMapping = buildServletMapping(engine);
        ServletRegistrationBean<EngineServlet> bean           = new ServletRegistrationBean<>(servlet, servletMapping);

        //bean.setMultipartConfig(multipartConfig);

        bean.setLoadOnStartup(1);
        return bean;
    }

    @Bean
    public ConfigurableServletWebServerFactory buildWebServerFactory() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        tomcat.addContextCustomizers(context -> context.setAllowCasualMultipartParsing(true));

        tomcat.addConnectorCustomizers(connector -> {
//            AbstractHttp11Protocol httpProtocol = (AbstractHttp11Protocol) connector.getProtocolHandler();
//            httpProtocol.setCompressibleMimeType("text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json");
//            httpProtocol.setCompression("1024");//compresses responses over 1KB.

            connector.setMaxPostSize(1024);
        });




        return tomcat;
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



}
