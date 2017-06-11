package io.arivera.oss.jchatops.responders;

import io.arivera.oss.jchatops.internal.ConversationContext;
import io.arivera.oss.jchatops.internal.ConversationManager;

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
import java.util.stream.Stream;

@Component
@Scope("singleton")
public class Responder {

  private static final Logger LOGGER = LoggerFactory.getLogger(Responder.class);

  private static final String[] SYSTEM_CONVERSATION_BEANS = {CancelConversationHandlers.RESET_BEAN_NAME};

  private final RTMClient rtmClient;
  private final Gson gson;
  private final ConversationManager conversationManager;

  @Autowired
  public Responder(RTMClient rtmClient, Gson gson,
                   ConversationManager conversationManager) {
    this.rtmClient = rtmClient;
    this.gson = gson;
    this.conversationManager = conversationManager;
  }

  private void resetConversation(Message originalMessage) {
    String user = originalMessage.getUser();
    String channel = originalMessage.getChannel();
    conversationManager.clearConversation(user, channel);
  }

  private void setConversationFollowUps(Response responseContext) {
    String user = responseContext.getOriginalMessage().getUser();
    String channel = responseContext.getOriginalMessage().getChannel();

    ConversationContext context = conversationManager.getConversation(user, channel)
        .orElse(new ConversationContext());

    List<String> beansToFollowUpWith = new ArrayList<>();
    beansToFollowUpWith.addAll(responseContext.getConversationBeansToFollowUpWith());
    beansToFollowUpWith.addAll(Arrays.asList(SYSTEM_CONVERSATION_BEANS));

    context.setNextConversationBeanNames(beansToFollowUpWith);
    conversationManager.saveConversation(user, channel, context);
  }

  /**
   * Submits the response.
   */
  public void submitResponse(Response responseContext) {
    if (responseContext.shouldResetConversation()) {
      resetConversation(responseContext.getOriginalMessage());
    }

    if (!responseContext.getConversationBeansToFollowUpWith().isEmpty()) {
      setConversationFollowUps(responseContext);
    }

    Stream<Message> slackMessages = responseContext.getMessages().stream()
        .map(msgData -> {
          Message slackMessage = new Message();
          slackMessage.setType("message");
          slackMessage.setText(msgData.getText());
          slackMessage.setChannel(
              msgData.getChannel().orElseThrow(() -> new IllegalStateException("No channel defined."))
          );
          return slackMessage;
        });

    submitRawMessage(slackMessages);
  }

  /**
   * Message will be sent "as is".
   */
  public void submitRawMessage(Stream<Message> messages) {
    messages
        .map(gson::toJson)
        .forEach(json -> {
          LOGGER.debug("Message to be submitted: {}", json);
          rtmClient.sendMessage(json);
        });
  }

}