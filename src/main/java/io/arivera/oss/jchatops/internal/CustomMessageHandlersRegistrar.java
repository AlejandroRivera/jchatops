package io.arivera.oss.jchatops.internal;

import io.arivera.oss.jchatops.MessageHandler;
import io.arivera.oss.jchatops.responders.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.classreading.MethodMetadataReadingVisitor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.PatternSyntaxException;
import javax.annotation.PostConstruct;

@Configuration
public class CustomMessageHandlersRegistrar {

  private static final Logger LOGGER = LoggerFactory.getLogger(CustomMessageHandlersRegistrar.class);

  private final AnnotationConfigEmbeddedWebApplicationContext applicationContext;

  private final Map<String, MessageHandler.FriendlyMessageHandler> allUserMessageHandlers;
  private final Map<String, MessageHandler.FriendlyMessageHandler> standaloneUserMessageHandlers;

  @Autowired
  public CustomMessageHandlersRegistrar(AnnotationConfigEmbeddedWebApplicationContext applicationContext) {
    this.applicationContext = applicationContext;

    this.allUserMessageHandlers = new HashMap<>();
    this.standaloneUserMessageHandlers = new HashMap<>();
  }

  @PostConstruct
  public void init() {
    registerMessageHandlers();
  }

  private void registerMessageHandlers() {
    ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
    String[] beanCandidates = beanFactory.getBeanDefinitionNames();

    Arrays.stream(beanCandidates)
        .forEach(beanName -> Optional.of(beanName)
            .map(beanFactory::getBeanDefinition)
            .map(BeanMetadataElement::getSource)
            .filter(Objects::nonNull)
            .filter(source -> source instanceof MethodMetadataReadingVisitor)
            .map(source -> (MethodMetadataReadingVisitor) source)
            .filter(source -> source.getReturnTypeName() != null)
            .filter(source -> {
                  try {
                    return Response.class.isAssignableFrom(Class.forName(source.getReturnTypeName()));
                  } catch (ClassNotFoundException e) {
                    return false;
                  }
                }
            )
            .map(source -> source.getAnnotationAttributes(MessageHandler.class.getName()))
            .map(annotationAttributes -> {
              try {
                return new MessageHandler.FriendlyMessageHandler(annotationAttributes);
              } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("Invalid Pattern found in bean '" + beanName + "'", e);
              }
            })
            .ifPresent(messageHandler -> {
                  LOGGER.debug("Detected MessageHandler in bean named '{}': {}", beanName, messageHandler);

                  allUserMessageHandlers.putIfAbsent(beanName, messageHandler);
                  if (!messageHandler.requiresConversation()) {
                    standaloneUserMessageHandlers.putIfAbsent(beanName, messageHandler);
                  }
                }
            )
        );
  }

  @Bean
  @Qualifier("all")
  public Map<String, MessageHandler.FriendlyMessageHandler> getAllUserMessageHandlers() {
    return allUserMessageHandlers;
  }

  @Bean
  @Qualifier("standalone")
  public Map<String, MessageHandler.FriendlyMessageHandler> getStandaloneUserMessageHandlers() {
    return standaloneUserMessageHandlers;
  }
}
