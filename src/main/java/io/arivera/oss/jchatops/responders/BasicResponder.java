package io.arivera.oss.jchatops.responders;

import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.rtm.RTMClient;
import com.google.gson.Gson;
import io.arivera.oss.jchatops.internal.ConversationContext;
import io.arivera.oss.jchatops.internal.ConversationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Scope("singleton")
public class BasicResponder implements Responder {

  private static final Logger LOGGER = LoggerFactory.getLogger(BasicResponder.class);

  private static final String[] SYSTEM_CONVERSATION_BEANS =
      {DefaultConversationHandlers.RESET_BEAN_NAME, DefaultConversationHandlers.UNEXPECTED_MSG_BEAN_NAME};

  private final RTMClient rtmClient;
  private final Gson gson;
  private final ConversationManager conversationManager;
  private final List<ResponseProcessor> responseProcessors;


  @Autowired
  public BasicResponder(RTMClient rtmClient, Gson gson,
                        List<ResponseProcessor> responseProcessors,
                        ConversationManager conversationManager) {
    this.rtmClient = rtmClient;
    this.gson = gson;
    this.responseProcessors = responseProcessors;
    this.conversationManager = conversationManager;
  }

  public void respondWith(Response responseContext) {
    if (responseContext.shouldResetConversation()) {
      resetConversation(responseContext.getOriginalMessage());
    }

    if (!responseContext.getConversationBeansToFollowUpWith().isEmpty()) {
      setConversationFollowUps(responseContext);
    }

    Response processedResponse = applyResponseProcessors(responseContext);
    respondWithRawMessage(processedResponse.getResponseMessage());
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

  private Response applyResponseProcessors(Response response) {
    for (ResponseProcessor responseProcessor : responseProcessors) {
      response = responseProcessor.apply(response);
    }
    return response;
  }

  /**
   * Message will be sent "as is".
   */
  public void respondWithRawMessage(Message message) {
    String jsonMessage = gson.toJson(message);

    LOGGER.info("Submitting response: {}", message.getText());

    LOGGER.debug("Message payload to be submitted: {}", jsonMessage);
    rtmClient.sendMessage(jsonMessage);
  }

}