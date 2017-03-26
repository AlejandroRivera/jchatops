package io.arivera.oss.jchatops.internal;

import com.github.seratch.jslack.api.rtm.RTMClient;
import io.arivera.oss.jchatops.MessageHandler;
import io.arivera.oss.jchatops.ResponseSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.GenericWebApplicationContext;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.annotation.PostConstruct;

@Component
@Scope("singleton")
public class CustomMessageHandlersRegistrar {

  private static final Logger LOGGER = LoggerFactory.getLogger(CustomMessageHandlersRegistrar.class);

  private final ApplicationContext applicationContext;

  private final Map<String, FriendlyMessageHandler> allUserMessageHandlers;
  private final Map<String, FriendlyMessageHandler> standaloneUserMessageHandlers;

  @Autowired
  public CustomMessageHandlersRegistrar(RTMClient rtmClient,
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
//      BeanDefinition beanDefinition = ((AnnotationConfigEmbeddedWebApplicationContext) applicationContext)    // AnnotationConfigEmbeddedWebApplicationContext
//          .getBeanDefinition(beanName);

      BeanDefinition beanDefinition = ((GenericWebApplicationContext) applicationContext)    // AnnotationConfigEmbeddedWebApplicationContext
          .getBeanDefinition(beanName);

//      Map annotationAttributes = ((MethodMetadataReadingVisitor) beanDefinition.getSource())
//          .getAnnotationAttributes(MessageHandler.class.getName());

//      String[] patterns = (String[]) annotationAttributes.get(MessageHandler.PATTERNS_FIELD_NAME);
//      MessageType[] messageTypes = (MessageType[]) annotationAttributes.get(MessageHandler.MESSAGE_TYPES_FIELD_NAME);

      Annotation[] annotations = ((StandardMethodMetadata) beanDefinition.getSource()).getIntrospectedMethod().getDeclaredAnnotations();

      Arrays.stream(annotations)
          .filter(annotation -> annotation instanceof MessageHandler)
          .findFirst()
          .map(annotation -> (MessageHandler) annotation)
          .ifPresent(messageHandler -> {

            if (allUserMessageHandlers.containsKey(beanName)) {
              throw new IllegalStateException("Bean with name '" + beanName + "' is already defined!");
            }

//            MessageHandler messageHandler = new MessageHandler(){
//              @Override
//              public Class<? extends Annotation> annotationType() {
//                return MessageHandler.class;
//              }
//
//              @Override
//              public String[] patterns() {
//                return patterns;
//              }
//
//              @Override
//              public MessageType[] messageTypes() {
//                return messageTypes;
//              }
//            };

            FriendlyMessageHandler messangeHandlerWrapper = new FriendlyMessageHandler(messageHandler);
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

  public Map<String, FriendlyMessageHandler> getAllUserMessageHandlers() {
    return allUserMessageHandlers;
  }

  public Map<String, FriendlyMessageHandler> getStandaloneUserMessageHandlers() {
    return standaloneUserMessageHandlers;
  }
}
