package io.arivera.oss.jchatops.internal;

import io.arivera.oss.jchatops.ConversationData;
import io.arivera.oss.jchatops.MessageType;

import com.github.seratch.jslack.api.model.Message;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.regex.Matcher;

@Configuration
public class SlackMessageState {

  static ThreadLocal<MessageType> currentMessageType = ThreadLocal.withInitial(() -> null);
  static ThreadLocal<Message> currentMessage = ThreadLocal.withInitial(() -> null);
  static ThreadLocal<Matcher> currentPatternMatch = ThreadLocal.withInitial(() -> null);
  static ThreadLocal<ConversationData> currentConversationData = ThreadLocal.withInitial(() -> null);
  static ThreadLocal<ConversationContext> currentConversationContext = ThreadLocal.withInitial(() -> null);

  @Bean
  @Scope("prototype")
  public Message getMessage() {
    return currentMessage.get();
  }

  @Bean
  @Scope("prototype")
  public MessageType getMessageType() {
    return currentMessageType.get();
  }

  @Bean
  @Scope("prototype")
  public Matcher getPatternMatchResult() {
    return currentPatternMatch.get();
  }

  @Bean
  @Scope("prototype")
  public ConversationData getConversationData() {
    return currentConversationData.get();
  }

  @Bean
  @Scope("prototype")
  public ConversationContext getConversationContext() {
    return currentConversationContext.get();
  }


}
