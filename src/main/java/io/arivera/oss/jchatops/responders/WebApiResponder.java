package io.arivera.oss.jchatops.responders;

import io.arivera.oss.jchatops.internal.ConversationManager;
import io.arivera.oss.jchatops.internal.SlackMessageState;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.methods.SlackApiException;
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
public class WebApiResponder extends AbstractResponder {

  private static final Logger LOGGER = LoggerFactory.getLogger(WebApiResponder.class);

  private final Slack slack;

  @Autowired
  public WebApiResponder(Slack slack,
                         Gson gson,
                         SlackMessageState slackMessageState,
                         ConversationManager conversationManager) {
    super(slackMessageState, conversationManager, gson);
    this.slack = slack;
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
          try {
            LOGGER.debug("Posting message: {}", toJsonString(msg));
            ChatPostMessageResponse response = slack.methods().chatPostMessage(msg);
            LOGGER.debug("Response: {}", toJsonString(response));
            if (response.getWarning() != null) {
              LOGGER.warn("Message posting response contained warning: {}", response.getWarning());
            }
            if (response.getError() != null) {
              LOGGER.error("Message posting response contained warning: {}", response.getWarning());
            }
          } catch (IOException | SlackApiException e) {
            LOGGER.error("Exception submitting msg: {}", toJsonString(msg), e);
          }
        });
  }
}