package io.arivera.oss.jchatops.internal;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.methods.MethodsClient;
import com.github.seratch.jslack.api.methods.SlackApiException;
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
import javax.annotation.PreDestroy;
import javax.websocket.DeploymentException;

@Configuration
public class SlackApi {

  private static final Logger LOGGER = LoggerFactory.getLogger(SlackApi.class);

  protected RTMClient rtm;

  @Bean("slackToken")
  @Scope("singleton")
  public String getSlackToken(@Value("${slack.token}") String slackToken){
    return slackToken;
  }

  @Bean
  @Scope("singleton")
  public SlackHttpClient getSlackHttpClient() {
    return new SlackHttpClient();
  }

  @Autowired
  @Bean
  @Scope("singleton")
  public Slack getSlackInstance(SlackHttpClient httpClient) {
    return Slack.getInstance(httpClient);
  }

  @Autowired
  @Bean
  @Scope("singleton")
  public RTMClient SlackRealTimeMessaging(RTMStartResponse rtmStartResponse) {
    String wssUrl = rtmStartResponse.getUrl();
    try {
      rtm = new RTMClient(wssUrl);
      rtm.connect();
      LOGGER.info("Slack RTM connection established");
      return rtm;
    } catch (URISyntaxException e) {
      throw new IllegalStateException("URL received from RTM Start response is invalid: " + wssUrl, e);
    } catch (IOException | DeploymentException e) {
      throw new IllegalStateException("Could not connec to RTM with URL: " + wssUrl , e);
    }
  }

  /**
   * Bean containing global Slack state, as returned by the RTM connection response.
   */
  @Bean
  @Scope("singleton")
  public RTMStartResponse getRtmState(@Value("${slack.token}") String slackToken, Slack slack) {
    try {
      MethodsClient methods = slack.methods();
      return methods.rtmStart(
          RTMStartRequest.builder()
              .token(slackToken)
              .noUnreads(true)    // improves performance by avoiding to retrieve unread counts
              .mpimAware(false)   // multiparty IMs, when false, they'll be shown as groups.
              .build());
    } catch (IOException | SlackApiException e) {
      throw new IllegalStateException("Couldn't fetch RTM API WebSocket endpoint. Ensure the 'slack.token' value is correct.", e);
    }
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
