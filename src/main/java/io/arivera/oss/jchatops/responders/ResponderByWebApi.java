package io.arivera.oss.jchatops.responders;

import io.arivera.oss.jchatops.SlackMessage;
import io.arivera.oss.jchatops.internal.ConversationManager;
import io.arivera.oss.jchatops.internal.SlackMessageState;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.response.chat.ChatPostMessageResponse;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.rtm.RTMClient;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Qualifier("web-api")
@Scope("singleton")
public class ResponderByWebApi extends AbstractResponder {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResponderByWebApi.class);

  private final Slack slack;
  private final String slackToken;
  private final Gson gson;

  @Autowired
  public ResponderByWebApi(Slack slack, @Qualifier("slackToken") String slackToken,
                           Gson gson,
                           SlackMessageState slackMessageState,
                           ConversationManager conversationManager) {
    super(slackMessageState, conversationManager);
    this.slack = slack;
    this.slackToken = slackToken;
    this.gson = gson;
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
        .forEach(msg -> {
          ChatPostMessageRequest msgToPost = ChatPostMessageRequest.builder()
              .token(slackToken)
              .asUser(true)
              .attachments(msg.getAttachments())
              .channel(msg.getChannel())
              .threadTs(msg.getThreadTs())
              .text(msg.getText())
              .replyBroadcast(msg instanceof SlackMessage && ((SlackMessage) msg).isReplyBroadcast())
              .build();
          try {
            LOGGER.debug("Posting message: {}", gson.toJson(msgToPost));
            ChatPostMessageResponse response = slack.methods().chatPostMessage(msgToPost);
            LOGGER.debug("Response: {}", gson.toJson(msgToPost));
            if (response.getWarning() != null) {
              LOGGER.warn("Message posting response contained warning: {}", response.getWarning());
            }
            if (response.getError() != null) {
              LOGGER.error("Message posting response contained warning: {}", response.getWarning());
            }
          } catch (IOException | SlackApiException e) {
            LOGGER.error("Exception submitting msg: {}", gson.toJson(msgToPost), e);
          }
        });
  }
}