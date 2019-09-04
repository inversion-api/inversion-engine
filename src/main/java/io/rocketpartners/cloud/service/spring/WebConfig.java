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
 */
package io.rocketpartners.cloud.service.spring;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableWebMvc
public class WebConfig extends WebMvcConfigurerAdapter
{

   String[] allowedHeaders = new String[]{"content-length", "referer", "accept-language", "origin", "x-auth-token", "accept", "authorization", "host", "connection", "content-type", "accept-encoding", "Content-Type", "user-agent", "X-Application-Context"};

   @Override
   public void addCorsMappings(CorsRegistry registry)
   {
      registry//
              .addMapping("/**")//
              .allowedOrigins("*")//
              .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")//
              .allowedHeaders(allowedHeaders)//
              .exposedHeaders(allowedHeaders);

      // FYI.. we needed the headers in the exposedHeaders to be set otherwise angular's http client will not make them available to you.
   }
}
