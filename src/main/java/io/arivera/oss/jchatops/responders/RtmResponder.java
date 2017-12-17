package io.arivera.oss.jchatops.responders;

import io.arivera.oss.jchatops.SlackMessage;
import io.arivera.oss.jchatops.internal.ConversationManager;
import io.arivera.oss.jchatops.internal.SlackMessageState;

import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.rtm.RTMClient;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Qualifier("rtm")
@Scope("singleton")
public class RtmResponder extends AbstractResponder {

  private static final Logger LOGGER = LoggerFactory.getLogger(RtmResponder.class);

  private final RTMClient rtmClient;

  @Autowired
  public RtmResponder(RTMClient rtmClient, Gson gson,
                      SlackMessageState slackMessageState,
                      ConversationManager conversationManager) {
    super(slackMessageState, conversationManager, gson);
    this.rtmClient = rtmClient;
  }

  /**
   * Submits the response to Slack.
   * <p>
   * <p>Converts each {@link Message} to JSON and submits it back using {@link RTMClient#sendMessage(String)}</p>
   */
  @Override
  public void submitResponse(Response responseContext) {
    if (responseContext.shouldResetConversation()) {
      resetConversation(getOriginalMessage());
    }

    if (!responseContext.getConversationBeansToFollowUpWith().isEmpty()) {
      setConversationFollowUps(responseContext);
    }

    responseContext.getSlackResponseMessages()
        .map(this::toRtmMessage)
        .map(this::toJsonString)
        .forEach(json -> {
          LOGGER.debug("Message to be submitted via RTM: {}", json);
          rtmClient.sendMessage(json);
        });
  }

  private SlackMessage toRtmMessage(ChatPostMessageRequest chatPostMessageRequest) {
    return SlackMessage.builder()
        .setText(chatPostMessageRequest.getText())
        .setChannel(chatPostMessageRequest.getChannel())
        .setAttachments(chatPostMessageRequest.getAttachments())    // in case RTM ever support attachments
        .setThreadTs(chatPostMessageRequest.getThreadTs())
        .setReplyBroadcast(chatPostMessageRequest.isReplyBroadcast())
        .build();
  }

}