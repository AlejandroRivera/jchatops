package io.arivera.oss.jchatops.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanRegistryConfiguration {

  @Bean
  @Autowired
  public BeanDefinitionRegistry getBeanDefinitionRegsitry(ApplicationContext applicationContext) {
    if (applicationContext instanceof AnnotationConfigEmbeddedWebApplicationContext){
      return (BeanDefinitionRegistry) ((AnnotationConfigEmbeddedWebApplicationContext) applicationContext).getBeanFactory();
    } else {
      return (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
    }
  }
}
