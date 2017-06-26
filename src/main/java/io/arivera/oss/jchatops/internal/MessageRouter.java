package io.arivera.oss.jchatops.internal;

import static io.arivera.oss.jchatops.misc.MoreCollectors.toLinkedMap;

import io.arivera.oss.jchatops.MessageFilter;
import io.arivera.oss.jchatops.MessageHandler;
import io.arivera.oss.jchatops.MessageType;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;

@Component
@Scope("prototype")
public class MessageRouter extends MessageFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(MessageRouter.class);

  private final Optional<ConversationContext> conversation;
  private final MessageType currentMessageType;
  private final Map<String, MessageHandler.FriendlyMessageHandler> allMessageHandlers;
  private final Map<String, MessageHandler.FriendlyMessageHandler> standaloneMessageHandlers;
  private final ApplicationContext applicationContext;

  @Autowired
  public MessageRouter(Optional<ConversationContext> conversation,
                       MessageType currentMessageType,
                       @Qualifier("all") Map<String, MessageHandler.FriendlyMessageHandler> allMessageHandlers,
                       @Qualifier("standalone") Map<String, MessageHandler.FriendlyMessageHandler> standaloneMessageHandlers,
                       ApplicationContext applicationContext) {
    super(Integer.MAX_VALUE);
    this.conversation = conversation;
    this.currentMessageType = currentMessageType;
    this.allMessageHandlers = allMessageHandlers;
    this.standaloneMessageHandlers = standaloneMessageHandlers;
    this.applicationContext = applicationContext;
  }

  @Override
  public Optional<Response> apply(Message message) {

    Optional<Response> matchedResponseSupplier = Optional.empty();

    Map<String, MessageHandler.FriendlyMessageHandler> messageHandlersToInspect = getMessageHandlersToInspect(conversation);

    for (Map.Entry<String, MessageHandler.FriendlyMessageHandler> entry : messageHandlersToInspect.entrySet()) {
      String beanName = entry.getKey();

      MessageHandler.FriendlyMessageHandler messageHandler = entry.getValue();
      LOGGER.debug("Inspecting bean '{}' to see if it can process the message...", beanName);

      if (shouldSkipCurrentBean(beanName, currentMessageType, conversation, messageHandler)) {
        continue;
      }

      Optional<Matcher> matchingMatcher = messageHandler.getCompiledPatterns().stream()
          .map(pattern -> pattern.matcher(message.getText()))
          .filter(Matcher::matches)
          .findFirst();

      if (matchingMatcher.isPresent()) {
        LOGGER.debug("Pattern '{}' matched in bean '{}'", matchingMatcher.get().pattern().pattern(), beanName);
        SlackMessageState.currentMatchedPattern.set(matchingMatcher.get());

        Response bean = applicationContext.getBean(beanName, Response.class);
        matchedResponseSupplier = Optional.of(bean);
        break;
      } else {
        LOGGER.debug("Discarding bean '{}' because the patterns '{}' don't match the received message.",
            beanName, messageHandler.getCompiledPatterns());
      }
    }

    return matchedResponseSupplier;
  }

  @Override
  public void setNextFilter(MessageFilter nextFilter) {
    throw new IllegalStateException("This filter should always be the last one.");
  }

  private Map<String, MessageHandler.FriendlyMessageHandler> getMessageHandlersToInspect(
      Optional<ConversationContext> conversation) {
    return conversation
        .map(ConversationContext::getNextConversationBeanNames)
        .map(beanNames -> beanNames.stream()
            .collect(toLinkedMap(Function.identity(), allMessageHandlers::get)))
        .orElse(standaloneMessageHandlers);
  }

  private boolean shouldSkipCurrentBean(String beanName,
                                        MessageType currentMessageType,
                                        Optional<ConversationContext> conversation,
                                        MessageHandler.FriendlyMessageHandler messageHandler) {

    if (conversation.isPresent() && !conversation.get().getNextConversationBeanNames().contains(beanName)) {
      LOGGER.debug("Bean '{}' is not in the list of beans allowed for the current conversation: {}",
          beanName, conversation.get().getNextConversationBeanNames());
      return true;
    }

    if (!conversation.isPresent() && messageHandler.requiresConversation()) {
      LOGGER.debug("Bean '{}' requires a conversation to have already been started", beanName);
      return true;
    }

    if (!messageHandler.getMessageTypes().contains(currentMessageType)) {
      LOGGER.debug("Bean '{}' can only handle messages of type {}, not : {}",
          beanName, messageHandler.getMessageTypes(), currentMessageType);
      return true;
    }

    return false;
  }
}
