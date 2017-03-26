package io.arivera.oss.jchatops.internal;

import static io.arivera.oss.jchatops.misc.MoreCollectors.toLinkedMap;

import com.github.seratch.jslack.api.model.Im;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.User;
import com.github.seratch.jslack.api.rtm.RTMClient;
import com.github.seratch.jslack.api.rtm.RTMMessageHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.arivera.oss.jchatops.MessageType;
import io.arivera.oss.jchatops.ResponseSupplier;
import io.arivera.oss.jchatops.responders.BasicResponder;
import io.arivera.oss.jchatops.responders.NoOpResponder;
import io.arivera.oss.jchatops.responders.Responder;
import io.arivera.oss.jchatops.responders.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
@Scope("singleton")
public class SlackRtmMessagesHandler implements RTMMessageHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(SlackRtmMessagesHandler.class);

  private final RTMClient rtmClient;
  private final Gson gson;
  private final ApplicationContext applicationContext;
  private final Map<String, Im> ims;
  private final User bot;

  private final ConversationManager conversationManager;

  private final NoOpResponder noOpResponder;
  private final BasicResponder basicResponder;
  private final CustomMessageHandlersRegistrar customMessageHandlersRegistrar;

  @Autowired
  public SlackRtmMessagesHandler(RTMClient rtmClient,
                                 ApplicationContext applicationContext,
                                 GsonSupplier gsonSupplier,
                                 Map<String, Im> ims,
                                 User bot, ConversationManager conversationManager,
                                 CustomMessageHandlersRegistrar customMessageHandlersRegistrar,
                                 BasicResponder basicResponder,
                                 NoOpResponder noOpResponder) {
    this.rtmClient = rtmClient;
    this.gson = gsonSupplier.get();
    this.applicationContext = applicationContext;
    this.ims = ims;
    this.bot = bot;
    this.basicResponder = basicResponder;
    this.noOpResponder = noOpResponder;
    this.customMessageHandlersRegistrar = customMessageHandlersRegistrar;
    this.conversationManager = conversationManager;
  }

  @PostConstruct
  public void init() {
    rtmClient.addMessageHandler(this);
  }

  @PreDestroy
  public void close() {
    rtmClient.removeMessageHandler(this);
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
    LOGGER.debug("'{}' message received: {}", currentMessageType, rawMessage);

    Message message = preProcessMessage(rawMessage, currentMessageType);

    SlackMessageState.currentMessage.set(message);

    Optional<ConversationContext> conversation = conversationManager.getConversation(message.getUser(), message.getChannel());
    conversation.ifPresent(conversationContext -> SlackMessageState.currentConversation.set(conversationContext.getData()));

    Optional<ResponseSupplier> matchedResponder = Optional.empty();

    Map<String, FriendlyMessageHandler> map = getBeansAndSettingsToInspect(conversation);

    for (Map.Entry<String, FriendlyMessageHandler> entry : map.entrySet()) {
      String beanName = entry.getKey();

      FriendlyMessageHandler messageHandler = entry.getValue();
      LOGGER.debug("Inspecting bean '{}' to see if it can process the message...", beanName);

      if (shouldSkipCurrentBean(beanName, currentMessageType, conversation, messageHandler)) continue;

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
    } else {
      responder = noOpResponder;
    }
    responder.respondWith(response);
  }

  private boolean shouldSkipCurrentBean(String beanName,
                                        MessageType currentMessageType,
                                        Optional<ConversationContext> conversation,
                                        FriendlyMessageHandler messageHandler) {

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

  private Map<String, FriendlyMessageHandler> getBeansAndSettingsToInspect(Optional<ConversationContext> conversation) {
    return conversation
        .map(ConversationContext::getNextConversationBeanNames)
        .map(beanNames -> beanNames.stream().collect(
            toLinkedMap(Function.identity(), customMessageHandlersRegistrar.getAllUserMessageHandlers()::get)))
        .orElse(customMessageHandlersRegistrar.getStandaloneUserMessageHandlers());
  }

  private Message preProcessMessage(Message message, MessageType currentMessageType) {
    if (currentMessageType == MessageType.TAGGED) {
      message.setText(message.getText().replaceAll("^\\s*<@" + bot.getId() + ">\\S*", "").trim());
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
