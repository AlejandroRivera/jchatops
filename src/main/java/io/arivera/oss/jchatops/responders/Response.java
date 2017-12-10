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
import java.util.Optional;
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
  private Optional<Boolean> asThread = Optional.empty();
  private boolean alsoPostToMainConversation;

  @Autowired
  public Response(BeanDefinitionRegistry beanDefinitionRegistry) {
    this.beanDefinitionRegistry = beanDefinitionRegistry;
  }

  public Response message(String... messages) {
    return this.message(Arrays.asList(messages));
  }

  public Response message(List<String> messages) {
    return this.message(messages.stream());
  }

  public Response message(Stream<String> messages) {
    return this.messages(messages.map(msg -> SlackMessage.builder().setText(msg).build()));
  }

  public Response messages(Message... message) {
    return this.messages(Arrays.stream(message));
  }

  public Response messages(Stream<Message> message) {
    this.messages = message;
    return this;
  }

  /**
   * Indicates that the current conversation is considered finished and cached data can be cleared.
   */
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

  /**
   * @see #inThread(boolean)
   */
  public Response inThread() {
    return inThread(true);
  }

  /**
   * Post the message in a thread instead of in the main conversation (channel, group, ...).
   *
   * <p>By default, messages received in a thread will be responded in the same thread. There's no need to use this method
   * for those scenarios. You should use this if you want to start a new thread instead
   *
   * @see #alsoPostToMainConversation
   * @see #alsoPostToMainConversation(boolean)
   */
  public Response inThread(boolean enabled) {
    this.asThread = Optional.of(enabled);
    return this;
  }

  public Optional<Boolean> shouldRespondInThread() {
    return asThread;
  }

  /**
   * @see #alsoPostToMainConversation(boolean)
   */
  public Response alsoPostToMainConversation() {
    return alsoPostToMainConversation(true);
  }

  /**
   * When a message is posted as part of a thread, this can be used to signal that the same message should be made visible to
   * everyone in the main conversation.
   */
  public Response alsoPostToMainConversation(boolean enabled) {
    this.alsoPostToMainConversation = enabled;
    return this;
  }

  public boolean shouldBroadcastInChannel() {
    return this.alsoPostToMainConversation;
  }
}