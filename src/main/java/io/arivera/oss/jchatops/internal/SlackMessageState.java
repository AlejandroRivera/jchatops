package io.arivera.oss.jchatops.internal;

import io.arivera.oss.jchatops.ConversationData;
import io.arivera.oss.jchatops.MessageType;
import io.arivera.oss.jchatops.annotations.MessageGraph;
import io.arivera.oss.jchatops.annotations.MessageHandler;

import com.github.seratch.jslack.api.model.Channel;
import com.github.seratch.jslack.api.model.Group;
import com.github.seratch.jslack.api.model.Im;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.regex.Matcher;

@Configuration
public class SlackMessageState {

  static ThreadLocal<MessageType> currentMessageType = ThreadLocal.withInitial(() -> null);
  static ThreadLocal<Message> currentMessage = ThreadLocal.withInitial(() -> null);
  static ThreadLocal<User> currentSender = ThreadLocal.withInitial(() -> null);
  static ThreadLocal<Im> currentInstantMessage = ThreadLocal.withInitial(() -> null);
  static ThreadLocal<Group> currentGroup = ThreadLocal.withInitial(() -> null);
  static ThreadLocal<Channel> currentChannel = ThreadLocal.withInitial(() -> null);
  static ThreadLocal<Matcher> currentMatchedPattern = ThreadLocal.withInitial(() -> null);
  static ThreadLocal<ConversationData> currentConversationData = ThreadLocal.withInitial(() -> null);
  static ThreadLocal<ConversationContext> currentConversationContext = ThreadLocal.withInitial(() -> null);

  /**
   * @return Message received by this Slack bot.
   *
   * @see #channel()
   * @see #messageSender()
   */
  @Bean
  @Scope("prototype")
  @MessageGraph
  public Message getMessage() {
    return currentMessage.get();
  }

  /**
   * @return Type of message received by this Slack bot.
   *
   * @see MessageType#PRIVATE
   * @see MessageType#PUBLIC
   * @see MessageType#TAGGED
   */
  @Bean
  @Scope("prototype")
  @MessageGraph
  public MessageType messageType() {
    return currentMessageType.get();
  }

  /**
   * @return Matcher instance that contains an already evaluated expression for the received message.
   *     This is useful when trying to extract information from a pattern, particularly using named patterns.
   *
   * @see MessageHandler#patterns()
   * @see Matcher#group(String)
   */
  @Bean
  @Scope("prototype")
  @MessageGraph
  public Matcher matchedPattern() {
    return currentMatchedPattern.get();
  }

  /**
   * @return User information about the sender of the received message.
   *
   * @see SlackBotState#getBot()
   */
  @Bean
  @Scope("prototype")
  @MessageGraph
  public User messageSender() {
    return currentSender.get();
  }

  /**
   * @return Channel where the message was received or {@code null} if message wasn't received in a Channel.
   *
   * @see #instantMessage()
   * @see #groupChat()
   */
  @Bean
  @Scope("prototype")
  @MessageGraph
  public Channel channel() {
    return currentChannel.get();
  }

  /**
   * Instant Message details where the message was received or {@code null} if message was received elsewhere.
   *
   * @see #channel()
   * @see #groupChat()
   */
  @Bean
  @Scope("prototype")
  @MessageGraph
  public Im instantMessage() {
    return currentInstantMessage.get();
  }

  /**
   * Group chat information where the message was received, or {@code null} if the message was received elsewhere.
   *
   * @see #instantMessage()
   * @see #channel()
   */
  @Bean
  @Scope("prototype")
  @MessageGraph
  public Group groupChat() {
    return currentGroup.get();
  }

  @Bean
  @Scope("prototype")
  @MessageGraph
  public ConversationData conversationData() {
    return currentConversationData.get();
  }

  @Bean
  @Scope("prototype")
  @MessageGraph
  public ConversationContext conversationContext() {
    return currentConversationContext.get();
  }

}
