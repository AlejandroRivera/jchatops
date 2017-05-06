package io.arivera.oss.jchatops.internal;

import com.github.seratch.jslack.api.methods.response.rtm.RTMStartResponse;
import com.github.seratch.jslack.api.rtm.RTMClient;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import javax.annotation.PostConstruct;
import javax.websocket.DeploymentException;

@Configuration
public class SlackRealTimeMessagingMock extends SlackRealTimeMessaging {

  private static final Logger LOGGER = LoggerFactory.getLogger(SlackRealTimeMessagingMock.class);

  public SlackRealTimeMessagingMock() throws IOException, DeploymentException {
    super("fakeSlackToken!");
  }

  @PostConstruct
  @Override
  protected void init() throws IOException, DeploymentException {
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

    InputStream json = this.getClass().getClassLoader().getResource("slack_state.json").openStream();
    this.rtmStartResponse = new GsonSupplier().get().fromJson(new InputStreamReader(json), RTMStartResponse.class);

    LOGGER.info("Mocked Slack RTM connection.");
  }
}
