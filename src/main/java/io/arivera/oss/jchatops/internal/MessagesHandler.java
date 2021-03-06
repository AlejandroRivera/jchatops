package io.arivera.oss.jchatops.internal;

import io.arivera.oss.jchatops.MessageFilter;
import io.arivera.oss.jchatops.MessageType;
import io.arivera.oss.jchatops.annotations.Bot;
import io.arivera.oss.jchatops.annotations.BotGraph;
import io.arivera.oss.jchatops.responders.Responder;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Channel;
import com.github.seratch.jslack.api.model.Group;
import com.github.seratch.jslack.api.model.Im;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.User;
import com.github.seratch.jslack.api.rtm.RTMMessageHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
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
  private final Map<String, User> users;
  private final Map<String, Group> groups;
  private final Map<String, Channel> channels;

  private final User bot;

  private final ConversationManager conversationManager;

  private final Responder responder;

  @Autowired
  public MessagesHandler(ApplicationContext applicationContext,
                         Gson gson,
                         @Bot User bot,
                         @BotGraph Map<String, Im> ims,
                         @BotGraph Map<String, Channel> channels,
                         @BotGraph Map<String, Group> groups,
                         @BotGraph Map<String, User> users,
                         ConversationManager conversationManager,
                         Responder responder) {
    this.gson = gson;
    this.applicationContext = applicationContext;
    this.ims = ims;
    this.channels = channels;
    this.groups = groups;
    this.users = users;
    this.bot = bot;
    this.responder = responder;
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
    LOGGER.debug("Message JSON: {}", jsonMessage);

    JsonObject jsonObject = gson.fromJson(jsonMessage, JsonObject.class);
    if (jsonObject.get("type") == null) {
      LOGGER.debug("Message received without 'type'");
      return;
    } else if (jsonObject.get("subtype") != null) {
      // See "Message subtypes" in https://api.slack.com/events/message
      LOGGER.debug("Ignoring message since is of a subtype of message.");
      return;
    }

    Message message;
    try {
      message = gson.fromJson(jsonObject, Message.class);
    } catch (JsonSyntaxException e) {
      LOGGER.warn("Could not parse JSON from message: {}", jsonMessage, e);
      return;
    }
    if (message.getType().equalsIgnoreCase("error")) {
      LOGGER.warn("Message receive is reporting an error: {}", jsonMessage);
      return;
    } else if (!message.getType().equalsIgnoreCase("message")) {
      LOGGER.debug("Message received but it's not an actual 'message' but a '{}'", message.getType());
      return;
    }

    MessageType currentMessageType = extractMessageType(message);
    LOGGER.debug("'{}' message received: {}", currentMessageType, message);

    SlackMessageState.currentSender.set(users.get(message.getUser()));
    SlackMessageState.currentMessage.set(message);
    SlackMessageState.currentMessageType.set(currentMessageType);

    SlackMessageState.currentChannel.set(channels.get(message.getChannel()));
    SlackMessageState.currentInstantMessage.set(ims.get(message.getChannel()));
    SlackMessageState.currentGroup.set(groups.get(message.getChannel()));

    Optional<ConversationContext> conversation = conversationManager.getConversation(message);
    SlackMessageState.currentConversationContext.set(conversation.orElse(null));

    List<MessageFilter> filters = new ArrayList<>(applicationContext.getBeansOfType(MessageFilter.class).values());
    filters.sort(AnnotationAwareOrderComparator.INSTANCE);

    Iterator<MessageFilter> iterator = filters.iterator();
    MessageFilter firstFilter = iterator.next();
    MessageFilter lastFilter = firstFilter;
    while (iterator.hasNext()) {
      MessageFilter nextFilter = iterator.next();
      lastFilter.setNextFilter(nextFilter);
      lastFilter = nextFilter;
    }

    if (!(lastFilter instanceof MessageRouter)) {
      throw new IllegalStateException("Last filter in chain was " + lastFilter.getClass().getName()
                                      + " and should be: " + MessageRouter.class.getName());
    }

    Optional<Response> maybeResponse = firstFilter.apply(message);
    maybeResponse.ifPresent(responder::submitResponse);
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
