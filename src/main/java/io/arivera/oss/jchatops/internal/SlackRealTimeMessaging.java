package io.arivera.oss.jchatops.internal;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.impl.MethodsClientImpl;
import com.github.seratch.jslack.api.methods.request.rtm.RTMStartRequest;
import com.github.seratch.jslack.api.methods.response.rtm.RTMStartResponse;
import com.github.seratch.jslack.api.rtm.RTMClient;
import com.github.seratch.jslack.common.http.SlackHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.io.IOException;
import java.net.URISyntaxException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.websocket.DeploymentException;

@Configuration
public class SlackRealTimeMessaging {

  private static final Logger LOGGER = LoggerFactory.getLogger(SlackRealTimeMessaging.class);

  protected RTMClient rtm;
  protected RTMStartResponse rtmStartResponse;

  private final String slackToken;

  @Autowired
  public SlackRealTimeMessaging(@Value("${slack.token}") String slackToken) {
    this.slackToken = slackToken;
  }

  @PostConstruct
  protected void init() throws IOException, DeploymentException {
    Slack slack = Slack.getInstance();

    try {
      MethodsClientImpl methods = new MethodsClientImpl(new SlackHttpClient());
      RTMStartRequest startRequest = RTMStartRequest.builder()
          .token(slackToken)
          .noUnreads(true)    // improves performance by avoiding to retrieve unread counts
          .mpimAware(false)   // multiparty IMs, when false, they'll be shown as groups.
          .build();

      rtmStartResponse = methods.rtmStart(startRequest);

      String wssUrl = rtmStartResponse.getUrl();
      rtm = new RTMClient(wssUrl);
    } catch (SlackApiException | URISyntaxException e) {
      throw new IllegalStateException("Couldn't fetch RTM API WebSocket endpoint. Ensure the apiToken value is correct.");
    }

    rtm = slack.rtm(slackToken);
    rtm.connect();
    LOGGER.info("Slack RTM connection established");
  }

  @Bean
  @Scope("singleton")
  protected RTMClient getRtmClient() {
    return rtm;
  }

  /**
   * Bean containing global Slack state, as returned by the RTM connection response.
   */
  @Bean
  @Scope("singleton")
  public RTMStartResponse getRtmState() {
    return rtmStartResponse;
  }

  @PreDestroy
  protected void finish() {
    if (rtm != null) {
      try {
        rtm.disconnect();
        LOGGER.info("Slack RTM connection closed");
      } catch (IOException e) {
        LOGGER.warn("Could not disconnect from Slack's RTM", e);
      }
    }
  }

}
