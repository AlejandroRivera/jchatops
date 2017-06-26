package io.arivera.oss.jchatops.internal;

import io.arivera.oss.jchatops.ConversationData;
import io.arivera.oss.jchatops.MessageHandler;
import io.arivera.oss.jchatops.MessageType;

import com.github.seratch.jslack.api.model.Channel;
import com.github.seratch.jslack.api.model.Group;
import com.github.seratch.jslack.api.model.Im;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.User;
import org.springframework.beans.factory.annotation.Qualifier;
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
  @Qualifier("messageGraph")
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
  @Qualifier("messageGraph")
  public MessageType messageType() {
    return currentMessageType.get();
  }

  /**
   * @return Matcher instance that contains an already evaluated expression for the received message.
   * This is useful when trying to extract information from a pattern, particularly using named patterns.
   *
   * @see MessageHandler#patterns()
   * @see Matcher#group(String)
   */
  @Bean
  @Scope("prototype")
  @Qualifier("messageGraph")
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
  @Qualifier("messageGraph")
  public User messageSender() {
    return currentSender.get();
  }

  /**
   * @return Channel where the message was received or {@code null} if message wasn't received in a Channel
   * (for example, if received as a Direct message)
   *
   * @see #instantMessage()
   * @see #groupChat()
   */
  @Bean
  @Scope("prototype")
  @Qualifier("messageGraph")
  public Channel channel() {
    return currentChannel.get();
  }

  /**
   * @return Instant Message details where the message was received or {@code null} if message was received elsewhere
   * (for example, in a Channel)
   *
   * @see #channel()
   * @see #groupChat()
   */
  @Bean
  @Scope("prototype")
  @Qualifier("messageGraph")
  public Im instantMessage() {
    return currentInstantMessage.get();
  }

  /**
   * @return Group chat information where the message was received, or {@code null} if the message was received elsewhere
   * (for example, in a Channel or as a Direct Message)
   *
   * @see #instantMessage()
   * @see #channel()
   */
  @Bean
  @Scope("prototype")
  @Qualifier("messageGraph")
  public Group groupChat() {
    return currentGroup.get();
  }

  @Bean
  @Scope("prototype")
  @Qualifier("messageGraph")
  public ConversationData conversationData() {
    return currentConversationData.get();
  }

  @Bean
  @Scope("prototype")
  @Qualifier("messageGraph")
  public ConversationContext conversationContext() {
    return currentConversationContext.get();
  }

}
