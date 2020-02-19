package io.inversion.cloud.service.spring.config;

import io.inversion.cloud.service.Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import static org.springframework.beans.factory.config.AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR;

/**
 *
 */
public class InversionRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, BeanFactoryAware
{
    private static final Logger log = LoggerFactory.getLogger(InversionRegistrar.class) ;

    Environment environment ;
    BeanFactory beanFactory ;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry)
    {
        GenericBeanDefinition bean = new GenericBeanDefinition() ;
        bean.setBeanClass(Engine.class);
        bean.setAutowireMode(AUTOWIRE_CONSTRUCTOR);
        //bean.setLazyInit(false);
        //bean.setInitMethodName("startup");
        bean.setDestroyMethodName("destroy");
        registry.registerBeanDefinition("engine", bean);
    }

    @Override
    public void setEnvironment(Environment environment)
    {
        this.environment = environment ;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException
    {
        this.beanFactory = beanFactory ;
    }
}
