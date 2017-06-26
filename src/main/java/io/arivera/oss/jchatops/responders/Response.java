package io.arivera.oss.jchatops.responders;

import io.arivera.oss.jchatops.SlackMessage;

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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Scope("prototype")
public class Response {

  private final BeanDefinitionRegistry beanDefinitionRegistry;

  private Stream<Message> messages;
  private boolean resetConversation;
  private List<String> conversationBeansToFollowUpWith = new ArrayList<>(0);

  @Autowired
  public Response(BeanDefinitionRegistry beanDefinitionRegistry) {
    this.beanDefinitionRegistry = beanDefinitionRegistry;
  }

  public Response message(String... messages) {
    return this.message(Arrays.asList(messages));
  }

  public Response message(List<String> messages) {
    return this.message(
        messages.stream()
    );
  }

  public Response message(Stream<String> messages) {
    return this.messages(messages.map(msg ->
        SlackMessage.builder()
            .setText(msg)
            .build()
    ));
  }

  public Response messages(Message... message) {
    return this.messages(Arrays.stream(message));
  }

  public Response messages(Stream<Message> message) {
    this.messages = message;
    return this;
  }

  public Response resettingConversation() {
    resetConversation = true;
    return this;
  }

  public Stream<Message> getSlackResponseMessages() {
    return messages;
  }

  public Response wrapSlackMessages(Function<Stream<Message>, Stream<Message>> transformer) {
    Stream<Message> originalResponseMessages = getSlackResponseMessages();
    Stream<Message> transformedMessages = transformer.apply(originalResponseMessages);
    this.messages(transformedMessages);
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

}