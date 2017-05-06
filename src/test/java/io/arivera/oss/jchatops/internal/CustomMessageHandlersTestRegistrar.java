package io.arivera.oss.jchatops.internal;

import io.arivera.oss.jchatops.MessageHandler;
import io.arivera.oss.jchatops.ResponseSupplier;

import com.github.seratch.jslack.api.rtm.RTMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.web.context.support.GenericWebApplicationContext;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.annotation.PostConstruct;

@Configuration
public class CustomMessageHandlersTestRegistrar {

  private static final Logger LOGGER = LoggerFactory.getLogger(CustomMessageHandlersTestRegistrar.class);

  private final ApplicationContext applicationContext;

  private final Map<String, MessageHandler.FriendlyMessageHandler> allUserMessageHandlers;
  private final Map<String, MessageHandler.FriendlyMessageHandler> standaloneUserMessageHandlers;

  @Autowired
  public CustomMessageHandlersTestRegistrar(RTMClient rtmClient,
                                            ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;

    this.allUserMessageHandlers = new HashMap<>();
    this.standaloneUserMessageHandlers = new HashMap<>();
  }

  @PostConstruct
  public void init() {
    registerMessageHandlers();
  }

  private void registerMessageHandlers() {
    Map<String, ResponseSupplier> beans = applicationContext.getBeansOfType(ResponseSupplier.class);

    beans.forEach((beanName, bean) -> {

      BeanDefinition beanDefinition = ((GenericWebApplicationContext) applicationContext)
          .getBeanDefinition(beanName);

      StandardMethodMetadata methodMetadata = (StandardMethodMetadata) beanDefinition.getSource();
      Annotation[] annotations = methodMetadata.getIntrospectedMethod().getDeclaredAnnotations();

      Arrays.stream(annotations)
          .filter(annotation -> annotation instanceof MessageHandler)
          .findFirst()
          .map(annotation -> (MessageHandler) annotation)
          .ifPresent(messageHandler -> {

            if (allUserMessageHandlers.containsKey(beanName)) {
              throw new IllegalStateException("Bean with name '" + beanName + "' is already defined!");
            }

            MessageHandler.FriendlyMessageHandler messangeHandlerWrapper = new MessageHandler.FriendlyMessageHandler(messageHandler);
            String[] patterns = messageHandler.patterns();

            Arrays.stream(patterns)
                .forEach(pattern -> {
                  try {
                    Pattern compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                    messangeHandlerWrapper.getCompiledPatterns().add(compiledPattern);
                  } catch (PatternSyntaxException e) {
                    throw new IllegalArgumentException(
                        String.format("The patterns value '%s' found in '%s' of class '%s' is invalid.",
                            pattern, beanName, beanDefinition.getBeanClassName()), e
                    );
                  }
                });

            allUserMessageHandlers.putIfAbsent(beanName, messangeHandlerWrapper);
            if (!messangeHandlerWrapper.requiresConversation()) {
              standaloneUserMessageHandlers.putIfAbsent(beanName, messangeHandlerWrapper);
            }
            LOGGER.debug("Detected MessageHandler in bean named '{}': {}", beanName, messageHandler);
          });
    });
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
