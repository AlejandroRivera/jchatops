package io.arivera.oss.jchatops.internal;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.methods.response.rtm.RTMStartResponse;
import com.github.seratch.jslack.api.rtm.RTMClient;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.Objects;

@Configuration
public class SlackApiMock extends SlackApi {

  private static final Logger LOGGER = LoggerFactory.getLogger(SlackApiMock.class);

  @Bean
  @Override
  public RTMClient slackRealTimeMessaging(RTMStartResponse rtmStartResponse) {
    try {
      this.rtm = Mockito.spy(new RTMClient("http://fake.slack.com") {
        @Override
        public void sendMessage(String message) {
          // do nothing
        }
      });
    } catch (URISyntaxException e) {
      throw new RuntimeException("Should not happen!");
    }

    LOGGER.info("Mocked Slack RTM connection.");
    return rtm;
  }

  @Bean
  @Override
  public RTMStartResponse getRtmState(String slackToken, Slack slack) {
    try {
      InputStream json = Objects.requireNonNull(this.getClass().getClassLoader().getResource("slack_state.json")).openStream();
      return new GsonSupplier().get().fromJson(new InputStreamReader(json), RTMStartResponse.class);
    } catch (IOException e) {
      throw new IllegalStateException("Could not open fake slack state JSON file", e);
    }
  }
}
