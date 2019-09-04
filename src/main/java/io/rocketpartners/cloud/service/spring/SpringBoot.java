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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import io.rocketpartners.cloud.service.Service;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class SpringBoot
{


//   public static void main(String[] args)
//   {
//      try
//      {
//         //org.springframework.boot.SpringApplication.run(SpringBoot.class, args);
//         run((Service) null);
//      }
//      catch (Exception ex)
//      {
//         Utils.getCause(ex).printStackTrace();
//      }
//   }
//
//   public static void run(Api api)
//   {
//      run(new Service().withApi(api));
//   }

   public static void run(Service service)
   {
      SpringApplication.run(SpringBoot.class);
//      try
//      {
//         //SpringApplicationBuilder builder = new SpringApplicationBuilder(SpringBoot.class, Controller.class);
//         SpringApplicationBuilder builder = new SpringApplicationBuilder(SpringBoot.class);
//         builder.initializers(new ServiceInitializer(service));
//         builder.run();
//         //new SpringApplicationBuilder(SpringBoot.class).run();//initializers(new ServiceInitializer(service)).run();
//      }
//      catch (Exception ex)
//      {
//         Utils.getCause(ex).printStackTrace();
//         ex.printStackTrace();
//      }
   }
//
//
//   static class ServiceInitializer implements ApplicationContextInitializer
//   {
//      Service service;
//
//      public ServiceInitializer(Service service)
//      {
//         super();
//         this.service = service;
//      }
//
//      @Override
//      public void initialize(ConfigurableApplicationContext applicationContext)
//      {
//         applicationContext.getBeanFactory().registerSingleton("service", service);
//      }
//
//   }

}