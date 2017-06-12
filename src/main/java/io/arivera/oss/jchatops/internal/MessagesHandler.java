package io.arivera.oss.jchatops.internal;

import io.arivera.oss.jchatops.MessageFilter;
import io.arivera.oss.jchatops.MessageType;
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component("sync_handler")
@Scope("singleton")
public class MessagesHandler implements RTMMessageHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(MessagesHandler.class);

  private final Gson gson;
  private final ApplicationContext applicationContext;
  private final Map<String, Im> ims;
  private final User bot;

  private final ConversationManager conversationManager;

  private final Responder basicResponder;

  @Autowired
  public MessagesHandler(ApplicationContext applicationContext,
                         GsonSupplier gsonSupplier,
                         Map<String, Im> ims,
                         User bot,
                         ConversationManager conversationManager,
                         Responder basicResponder) {
    this.gson = gsonSupplier.get();
    this.applicationContext = applicationContext;
    this.ims = ims;
    this.bot = bot;
    this.basicResponder = basicResponder;
    this.conversationManager = conversationManager;
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

    Message message = gson.fromJson(jsonObject, Message.class);
    if (message.getType().equalsIgnoreCase("error")) {
      LOGGER.warn("Message receive is reporting an error: {}", jsonMessage);
      return;
    } else if (!message.getType().equalsIgnoreCase("message")) {
      LOGGER.debug("Message received but it's not an actual 'message': {}", jsonMessage);
      return;
    }

    MessageType currentMessageType = extractMessageType(message);
    LOGGER.info("'{}' message received: {}", currentMessageType, message);

    SlackMessageState.currentMessage.set(message);
    SlackMessageState.currentMessageType.set(currentMessageType);

    Optional<ConversationContext> conversation = conversationManager.getConversation(message.getUser(), message.getChannel());
    SlackMessageState.currentConversationContext.set(conversation.orElse(null));
    SlackMessageState.currentConversationData.set(conversation.map(ConversationContext::getData).orElse(null));

    List<MessageFilter> filters = new ArrayList<>(applicationContext.getBeansOfType(MessageFilter.class).values());
    filters.sort(AnnotationAwareOrderComparator.INSTANCE);

    Iterator<MessageFilter> iterator = filters.iterator();
    MessageFilter firstFilter = iterator.next();
    MessageFilter previousFilter = firstFilter;
    while(iterator.hasNext()) {
      MessageFilter nextFilter = iterator.next();
      previousFilter.setNextFilter(nextFilter);
      previousFilter = nextFilter;
    }

    if (!(previousFilter instanceof MessageRouter)) {
      throw new IllegalStateException("Last filter in chain was " + previousFilter.getClass().getName()
                                      + " and should be: " + MessageRouter.class.getName());
    }

    Optional<Response> maybeResponse = firstFilter.apply(message);
    maybeResponse.ifPresent(basicResponder::submitResponse);
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
