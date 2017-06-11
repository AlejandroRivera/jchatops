package io.arivera.oss.jchatops.responders;

import com.github.seratch.jslack.api.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Scope;
import org.springframework.core.type.MethodMetadata;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class Response {

  private final Message originalMessage;
  private final BeanDefinitionRegistry beanDefinitionRegistry;

  private List<MessageData> messages;
  private boolean resetConversation;
  private List<String> conversationBeansToFollowUpWith = new ArrayList<>(0);

  @Autowired
  public Response(Message originalMessage, BeanDefinitionRegistry beanDefinitionRegistry) {
    this.originalMessage = originalMessage;
    this.beanDefinitionRegistry = beanDefinitionRegistry;
  }

  public Response message(String... messages) {
    return this.message(Arrays.asList(messages));
  }

  public Response message(List<String> messages) {
    this.messages = messages.stream()
        .map(MessageData::new)
        .collect(Collectors.toList());
    return this;
  }

  public Response message(MessageData... message) {
    this.messages = Arrays.asList(message);
    return this;
  }

  public List<MessageData> getMessages() {
    return messages;
  }

  public Response resettingConversation() {
    resetConversation = true;
    return this;
  }

  /**
   * Defines the bean names that should be used to handle the response for this conversation.
   */
  public Response followingUpWith(String... beanNames) {
    this.conversationBeansToFollowUpWith = Arrays.stream(beanNames)
        .map(bean -> {
          BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(bean);
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

  public static class MessageData {

    private String text;
    private String channel;

    public MessageData(String text) {
      this.text = text;
    }

    public String getText() {
      return text;
    }

    public MessageData setText(String text) {
      this.text = text;
      return this;
    }

    public Optional<String> getChannel() {
      return Optional.ofNullable(channel);
    }

    public MessageData setChannel(String channel) {
      this.channel = channel;
      return this;
    }
  }
}