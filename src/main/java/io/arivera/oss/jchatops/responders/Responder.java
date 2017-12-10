package io.arivera.oss.jchatops.responders;

import io.arivera.oss.jchatops.internal.ConversationContext;
import io.arivera.oss.jchatops.internal.ConversationManager;
import io.arivera.oss.jchatops.internal.SlackMessageState;

import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.rtm.RTMClient;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@Scope("singleton")
public class Responder {

  private static final Logger LOGGER = LoggerFactory.getLogger(Responder.class);

  private static final String[] SYSTEM_CONVERSATION_BEANS = {CancelConversationHandlers.RESET_BEAN_NAME};

  private final RTMClient rtmClient;
  private final Gson gson;
  private final SlackMessageState slackMessageState;
  private final ConversationManager conversationManager;

  @Autowired
  public Responder(RTMClient rtmClient, Gson gson,
                   SlackMessageState slackMessageState,
                   ConversationManager conversationManager) {
    this.rtmClient = rtmClient;
    this.gson = gson;
    this.slackMessageState = slackMessageState;
    this.conversationManager = conversationManager;
  }

  private void resetConversation(Message originalMessage) {
    String user = originalMessage.getUser();
    String channel = originalMessage.getChannel();

    Optional<String> thread = Optional.ofNullable(originalMessage.getThreadTs())
        .filter(s -> !s.equalsIgnoreCase(originalMessage.getTs()));

    // TODO: Reset conversations in Threads too!
    conversationManager.clearConversation(user, channel, thread);
  }

  private void setConversationFollowUps(Response responseContext) {
    Message originalMessage = getOriginalMessage();

    ConversationManager.ConversationKey key = new ConversationManager.ConversationKey(
        originalMessage.getUser(), originalMessage.getChannel(), Optional.empty());

    ConversationContext context = conversationManager.getConversation(originalMessage).orElse(new ConversationContext(key));

    List<String> beansToFollowUpWith = new ArrayList<>();
    beansToFollowUpWith.addAll(responseContext.getConversationBeansToFollowUpWith());
    beansToFollowUpWith.addAll(Arrays.asList(SYSTEM_CONVERSATION_BEANS));

    context.setNextConversationBeanNames(beansToFollowUpWith);
    conversationManager.saveConversation(originalMessage, responseContext, context);
  }

  /**
   * Submits the response to Slack.
   *
   * <p>Converts each {@link Message} to JSON and submits it back using {@link RTMClient#sendMessage(String)}</p>
   */
  public void submitResponse(Response responseContext) {
    if (responseContext.shouldResetConversation()) {
      resetConversation(getOriginalMessage());
    }

    if (!responseContext.getConversationBeansToFollowUpWith().isEmpty()) {
      setConversationFollowUps(responseContext);
    }

    responseContext.getSlackResponseMessages()
        .map(gson::toJson)
        .forEach(json -> {
          LOGGER.debug("Message to be submitted: {}", json);
          rtmClient.sendMessage(json);
        });
  }

  private Message getOriginalMessage() {
    return slackMessageState.getMessage();
  }
}