package io.arivera.oss.jchatops.internal;

import static io.arivera.oss.jchatops.misc.MoreCollectors.toLinkedMap;

import io.arivera.oss.jchatops.CustomMessagePreProcessor;
import io.arivera.oss.jchatops.MessageHandler;
import io.arivera.oss.jchatops.MessageType;
import io.arivera.oss.jchatops.ResponseSupplier;
import io.arivera.oss.jchatops.responders.BasicResponder;
import io.arivera.oss.jchatops.responders.NoOpResponder;
import io.arivera.oss.jchatops.responders.Responder;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Im;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.User;
import com.github.seratch.jslack.api.rtm.RTMMessageHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;

@Component("sync_handler")
@Scope("singleton")
public class MessagesHandler implements RTMMessageHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(MessagesHandler.class);

  private final Gson gson;
  private final ApplicationContext applicationContext;
  private final Map<String, Im> ims;
  private final User bot;

  private final ConversationManager conversationManager;

  private final NoOpResponder noOpResponder;
  private final BasicResponder basicResponder;

  private final Map<String, MessageHandler.FriendlyMessageHandler> standaloneMessageHandlers;
  private final Map<String, MessageHandler.FriendlyMessageHandler> allMessageHandlers;
  private Optional<List<CustomMessagePreProcessor>> msgPreProcessors;

  @Autowired
  public MessagesHandler(ApplicationContext applicationContext,
                         GsonSupplier gsonSupplier,
                         Map<String, Im> ims,
                         User bot,
                         ConversationManager conversationManager,
                         @Qualifier("all") Map<String, MessageHandler.FriendlyMessageHandler> allMessageHandlers,
                         @Qualifier("standalone") Map<String, MessageHandler.FriendlyMessageHandler> standaloneMessageHandlers,
                         BasicResponder basicResponder,
                         NoOpResponder noOpResponder,
                         Optional<List<CustomMessagePreProcessor>> msgPreProcessors) {
    this.gson = gsonSupplier.get();
    this.applicationContext = applicationContext;
    this.ims = ims;
    this.bot = bot;
    this.basicResponder = basicResponder;
    this.noOpResponder = noOpResponder;
    this.conversationManager = conversationManager;
    this.standaloneMessageHandlers = standaloneMessageHandlers;
    this.allMessageHandlers = allMessageHandlers;
    this.msgPreProcessors = msgPreProcessors;
  }

  @Override
  public void handle(String jsonMessage) {
    try {
      doHandle(jsonMessage);
    } catch (RuntimeException e) {
      LOGGER.error("Uncaught exception processing message: {}", jsonMessage, e);
    }
  }

  private void doHandle(String jsonMessage) {
    JsonObject jsonObject = gson.fromJson(jsonMessage, JsonObject.class);
    if (jsonObject.get("type") == null) {
      LOGGER.debug("Message received without 'type': {}", jsonMessage);
      return;
    }

    Message rawMessage = gson.fromJson(jsonObject, Message.class);
    if (!rawMessage.getType().equalsIgnoreCase("message")) {
      LOGGER.debug("Message received but it's not an actual 'message': {}", jsonMessage);
      return;
    }

    MessageType currentMessageType = extractMessageType(rawMessage);
    LOGGER.info("'{}' message received: {}", currentMessageType, rawMessage);

    Message message = preProcessMessage(rawMessage, currentMessageType);

    SlackMessageState.currentMessage.set(message);

    Optional<ConversationContext> conversation = conversationManager.getConversation(message.getUser(), message.getChannel());
    conversation.ifPresent(conversationContext -> SlackMessageState.currentConversation.set(conversationContext.getData()));

    Optional<ResponseSupplier> matchedResponder = Optional.empty();

    Map<String, MessageHandler.FriendlyMessageHandler> map = getBeansAndSettingsToInspect(conversation);

    for (Map.Entry<String, MessageHandler.FriendlyMessageHandler> entry : map.entrySet()) {
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
        LOGGER.debug("Pattern '{}' matched in bean '{}'",
            matchingMatcher.get().pattern().pattern(), beanName);
        SlackMessageState.currentPatternMatch.set(matchingMatcher.get());

        ResponseSupplier bean = applicationContext.getBean(beanName, ResponseSupplier.class);
        matchedResponder = Optional.of(bean);
        break;
      } else {
        LOGGER.debug("Discarding bean '{}' because the patterns '{}' don't match the received message.",
            beanName, messageHandler.getCompiledPatterns());
      }
    }

    Responder responder = noOpResponder;
    Response response = null;

    if (matchedResponder.isPresent()) {
      response = matchedResponder.get().get();
      responder = basicResponder;
    }

    responder.respondWith(response);
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

  private Map<String, MessageHandler.FriendlyMessageHandler> getBeansAndSettingsToInspect(Optional<ConversationContext> conversation) {
    return conversation
        .map(ConversationContext::getNextConversationBeanNames)
        .map(beanNames -> beanNames.stream()
            .collect(toLinkedMap(Function.identity(), allMessageHandlers::get)))
        .orElse(standaloneMessageHandlers);
  }

  private Message preProcessMessage(Message message, MessageType currentMessageType) {
    if (currentMessageType == MessageType.TAGGED) {
      message.setText(message.getText().replaceAll("^\\s*<@" + bot.getId() + ">\\S*", "").trim());
    }

    if (msgPreProcessors.isPresent()) {
      for (CustomMessagePreProcessor preProcessor : msgPreProcessors.get()) {
        message = preProcessor.process(message, currentMessageType);
      }
    }

    return message;
  }

  private MessageType extractMessageType(Message message) {
    MessageType currentMessageType;
    if (ims.containsKey(message.getChannel())) {
      currentMessageType = MessageType.PRIVATE;
    } else if (message.getText().contains("<@" + bot.getId() + ">")) {
      currentMessageType = MessageType.TAGGED;
    } else {
      currentMessageType = MessageType.PUBLIC;
    }
    return currentMessageType;
  }

}
