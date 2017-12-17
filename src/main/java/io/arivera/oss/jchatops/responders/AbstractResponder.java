package io.arivera.oss.jchatops.responders;

import io.arivera.oss.jchatops.internal.ConversationContext;
import io.arivera.oss.jchatops.internal.ConversationManager;
import io.arivera.oss.jchatops.internal.SlackMessageState;

import com.github.seratch.jslack.api.model.Message;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public abstract class AbstractResponder implements Responder {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractResponder.class);
  private static final String[] SYSTEM_CONVERSATION_BEANS = {CancelConversationHandlers.RESET_BEAN_NAME};

  protected final SlackMessageState slackMessageState;
  protected final ConversationManager conversationManager;
  protected final Gson gson;

  public AbstractResponder(SlackMessageState slackMessageState, ConversationManager conversationManager, Gson gson) {
    this.slackMessageState = slackMessageState;
    this.conversationManager = conversationManager;
    this.gson = gson;
  }

  protected Message getOriginalMessage() {
    return slackMessageState.getMessage();
  }

  protected void resetConversation(Message originalMessage) {
    String user = originalMessage.getUser();
    String channel = originalMessage.getChannel();

    Optional<String> thread = Optional.ofNullable(originalMessage.getThreadTs())
        .filter(s -> !s.equalsIgnoreCase(originalMessage.getTs()));

    conversationManager.clearConversation(user, channel, thread);
    LOGGER.debug("Response will reset the conversation for this user+channel[+thread]");
  }

  protected void setConversationFollowUps(Response responseContext) {
    Message originalMessage = getOriginalMessage();

    ConversationManager.ConversationKey key = new ConversationManager.ConversationKey(
        originalMessage.getUser(), originalMessage.getChannel(), Optional.empty());

    ConversationContext context = conversationManager.getConversation(originalMessage).orElse(new ConversationContext(key));

    List<String> beansToFollowUpWith = new ArrayList<>();
    beansToFollowUpWith.addAll(responseContext.getConversationBeansToFollowUpWith());
    beansToFollowUpWith.addAll(Arrays.asList(SYSTEM_CONVERSATION_BEANS));

    context.setNextConversationBeanNames(beansToFollowUpWith);
    conversationManager.saveConversation(originalMessage, responseContext, context);

    LOGGER.debug("Next responses received the same user+channel[+thread] will be processed by: {}", beansToFollowUpWith);
  }

  protected String toJsonString(Object obj) {
    return gson.toJson(obj);
  }
}
