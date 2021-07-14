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

import io.inversion.Api;
import io.inversion.ApiException;
import io.inversion.Engine;
import io.inversion.utils.Config;
import io.inversion.utils.Configurator;
import io.inversion.utils.Utils;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.type.AnnotationMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.springframework.beans.factory.config.AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR;

/**
 * Sets an Engine bean in the spring registry and puts spring application properties into
 * the Inversion Config so you can put Inversion DI props into your spring application.properties
 * as a convenience instead of inversion.*properties.  Properties found in inversion*.properties
 * will still be used by Inversion before any spring properties with the same keys.
 *
 * @see Config
 */
public class InversionRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, BeanFactoryAware {

    static final Logger log = LoggerFactory.getLogger(InversionRegistrar.class);

    Environment environment = null;
    BeanFactory beanFactory = null;
    public static Api[] apis = null;

    public InversionRegistrar()
    {
        System.out.println("InversionRegistrar()<>");
    }


    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;

        if(apis != null && apis.length > 0) {
            ConfigurableBeanFactory config = (ConfigurableBeanFactory)beanFactory;
            for (Api api : apis) {
                String name = api.getName();
                if(name == null)
                    throw new ApiException("Your Api must have a non null 'name' property configured when running your APIs with SpringBoot.");

                config.registerSingleton(name, api);
            }
        };

    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            GenericBeanDefinition bean = new GenericBeanDefinition();
            bean.setBeanClass(Engine.class);
            bean.setAutowireMode(AUTOWIRE_CONSTRUCTOR);
            //bean.setLazyInit(false);
            //bean.setInitMethodName("startup");
            bean.setDestroyMethodName("shutdown");
            registry.registerBeanDefinition("engine", bean);
    }

    /**
     * Copies the Spring properties configuration into the Inversion configuration
     * so you can put your Inversion config properties into your spring application.properties files.
     * <p>
     * Any properties found in inversion*.properties files will still override any properties set in
     * spring files.
     *
     * @param environment the Spring injected environment
     */
    @Override
    public void setEnvironment(Environment environment) {

        this.environment = environment;

        setEnvironmentDefaults(environment);

        PropertiesConfiguration springConfig = new PropertiesConfiguration();

        final MutablePropertySources sources = ((AbstractEnvironment) environment).getPropertySources();
        StreamSupport.stream(sources.spliterator(), false)
                .filter(ps -> ps instanceof EnumerablePropertySource)
                .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
                .flatMap(Arrays::stream)
                .distinct()
                .filter(prop -> !(prop.contains("credentials") || prop.contains("password")))
                .forEach(prop -> springConfig.setProperty(prop, environment.getProperty(prop)));

        CompositeConfiguration  inversionConfig      = Config.getConfiguration();

        List<Configuration> toAddBack  = new ArrayList<>();
        for(int i=0; i<inversionConfig.getNumberOfConfigurations(); i++)
        {
            Configuration subConfig = inversionConfig.getConfiguration(i);
            String source = subConfig.getProperty("source") + "";
            if(!source.equals("null"))
            {
                source = Utils.substringAfter(source, "/");
                if(source.startsWith("inversion") && source.endsWith(".properties"))
                {
                    i--;
                    inversionConfig.removeConfiguration(subConfig);
                    toAddBack.add(subConfig);
                }
            }
        }

        inversionConfig.addConfigurationFirst(springConfig);
        for(int i=toAddBack.size()-1; i>=0; i--)
        {
            Configuration subConfig = toAddBack.get(i);
            inversionConfig.addConfigurationFirst(subConfig);
        }
    }

    protected void setEnvironmentDefaults(Environment env){

        log.info("Setting SpringBoot Defaults:");

        setEnvironmentDefault(env, "server.compression.enabled", "true");
        setEnvironmentDefault(env, "server.compression.mime-types", "text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json");
        setEnvironmentDefault(env, "server.compression.min-response-size", "2048");
        setEnvironmentDefault(env, "server.port", "8080");
        setEnvironmentDefault(env, "spring.servlet.multipart.enabled", "true");
        setEnvironmentDefault(env, "spring.servlet.multipart.max-file-size", "100MB");
        setEnvironmentDefault(env, "spring.servlet.multipart.max-request-size", "100MB");
    }

    protected void setEnvironmentDefault(Environment env, String prop, String value){

        String found = env.getProperty(prop);
        if(found == null){
            System.setProperty(prop, value);
            log.info("  - [SETTING]  " + prop + " = " + value);
        }
        else{
            log.info("  - [SKIPPING] " + prop + " = " + found);
        }
    }

}
