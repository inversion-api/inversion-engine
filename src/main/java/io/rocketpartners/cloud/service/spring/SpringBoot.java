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

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import io.rocketpartners.cloud.model.Api;
import io.rocketpartners.cloud.service.Service;
import io.rocketpartners.cloud.utils.Utils;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class SpringBoot
{
   //static Service service = null;

   public static void main(String[] args)
   {
      try
      {
         org.springframework.boot.SpringApplication.run(SpringBoot.class, args);
      }
      catch (Exception ex)
      {
         Utils.getCause(ex).printStackTrace();
      }
   }

   public static void run(Api api)
   {
      run(new Service().withApi(api));
   }

   public static void run(Service service)
   {
      new SpringApplicationBuilder(SpringBoot.class).initializers(new ServiceInitializer(service)).run();
   }

   static class ServiceInitializer implements ApplicationContextInitializer
   {

      Service service;

      public ServiceInitializer(Service service)
      {
         super();
         this.service = service;
      }

      @Override
      public void initialize(ConfigurableApplicationContext applicationContext)
      {
         applicationContext.getBeanFactory().registerSingleton("service", service);
      }

   }

}