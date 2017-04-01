package io.arivera.oss.jchatops.internal;

import io.arivera.oss.jchatops.ConversationData;

import com.github.seratch.jslack.api.model.Message;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.regex.Matcher;

@Configuration
public class SlackMessageState {

  static ThreadLocal<Message> currentMessage = ThreadLocal.withInitial(() -> null);
  static ThreadLocal<Matcher> currentPatternMatch = ThreadLocal.withInitial(() -> null);
  static ThreadLocal<ConversationData> currentConversation = ThreadLocal.withInitial(() -> null);

  @Bean
  @Scope("prototype")
  public Message getMessage() {
    return currentMessage.get();
  }

  @Bean
  @Scope("prototype")
  public Matcher getPatternMatchResult() {
    return currentPatternMatch.get();
  }

  @Bean
  @Scope("prototype")
  public ConversationData getConversationContext() {
    return currentConversation.get();
  }

}
