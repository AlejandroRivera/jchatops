package io.arivera.oss.jchatops.responders;

import com.github.seratch.jslack.api.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.core.type.MethodMetadata;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class Response {

  private final ApplicationContext applicationContext;
  private final Message originalMessage;

  private Message responseMessage;
  private boolean resetConversation;
  private List<String> conversationBeansToFollowUpWith = new ArrayList<>(0);

  @Autowired
  public Response(Message originalMessage, ApplicationContext applicationContext) {
    this.originalMessage = originalMessage;
    this.applicationContext = applicationContext;
  }

  public Response message(String message) {
    Message response = createResponseMessage(message);
    return message(response);
  }

  public Message getResponseMessage() {
    return responseMessage;
  }

  private Message createResponseMessage(String message) {
    Message response = new Message();
    response.setType("message");
    response.setText(message);
    return response;
  }

  public Response message(Message message) {
    responseMessage = message;
    return this;
  }

  public Response resettingConversation() {
    resetConversation = true;
    return this;
  }

  /**
   * Defines the bean names that should be used to handle the response for this conversation.
   */
  public Response followingUpWith(String... beanNames) {
    BeanDefinitionRegistry beanFactory;

    if (applicationContext instanceof AnnotationConfigEmbeddedWebApplicationContext){
      beanFactory = (BeanDefinitionRegistry)
          ((AnnotationConfigEmbeddedWebApplicationContext) applicationContext).getBeanFactory();
    } else {
      AutowireCapableBeanFactory autowireCapableBeanFactory = applicationContext.getAutowireCapableBeanFactory();
      beanFactory = (DefaultListableBeanFactory) autowireCapableBeanFactory;
    }

    this.conversationBeansToFollowUpWith = Arrays.stream(beanNames)
        .map(bean -> {
          BeanDefinition beanDefinition = beanFactory.getBeanDefinition(bean);
          MethodMetadata source = (MethodMetadata) beanDefinition.getSource();
          try {
            if (Response.class.isAssignableFrom(Class.forName(source.getReturnTypeName()))) {
              return bean;
            }
          } catch (ClassNotFoundException e) {
            // ignored
          }
          throw new IllegalArgumentException(
              "Bean '" + bean + "' of type " + Response.class.getSimpleName() + " not found.");
        })
        .collect(Collectors.toList());
    return this;
  }

  public boolean shouldResetConversation() {
    return resetConversation;
  }

  public List<String> getConversationBeansToFollowUpWith() {
    return conversationBeansToFollowUpWith;
  }

  public Message getOriginalMessage() {
    return originalMessage;
  }
}