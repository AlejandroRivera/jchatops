package io.arivera.oss.jchatops.internal;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.methods.MethodsClient;
import com.github.seratch.jslack.api.methods.request.rtm.RTMStartRequest;
import com.github.seratch.jslack.api.methods.response.rtm.RTMStartResponse;
import com.github.seratch.jslack.api.rtm.RTMClient;
import com.github.seratch.jslack.common.http.SlackHttpClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.io.IOException;
import javax.annotation.PreDestroy;

@Configuration
public class SlackApi {

  private static final Logger LOGGER = LoggerFactory.getLogger(SlackApi.class);

  protected RTMClient rtm;

  @Bean("slackToken")
  @Scope("singleton")
  public String getSlackToken(@Value("${slack.token}") String slackToken) {
    return slackToken;
  }

  /**
   * OkHttpClient that includes the Slack Token an Authorization HTTP header.
   */
  @Bean
  public OkHttpClient getOkHttpClient(@Qualifier("slackToken") String slackToken) {
    return new OkHttpClient.Builder()
            .addInterceptor(chain -> {
              Request request = chain.request();
              Request requestWithTokenInHeader = request.newBuilder()
                  .addHeader("Authorization", "Bearer " + slackToken)
                  .build();
              return chain.proceed(requestWithTokenInHeader);
            })
            .build();
  }

  @Bean
  @Scope("singleton")
  public SlackHttpClient getSlackHttpClient(OkHttpClient okHttpClient) {
    return new SlackHttpClient(okHttpClient);
  }

  @Autowired
  @Bean
  @Scope("singleton")
  public Slack getSlackInstance(SlackHttpClient httpClient) {
    return Slack.getInstance(httpClient);
  }

  /**
   * Returns an already connected {@link RTMClient}.
   */
  @Autowired
  @Bean
  @Scope("singleton")
  public RTMClient slackRealTimeMessaging(RTMStartResponse rtmStartResponse) {
    String wssUrl = rtmStartResponse.getUrl();
    try {
      rtm = new RTMClient(wssUrl);
      rtm.connect();
      LOGGER.info("Slack RTM connection established");
      return rtm;
    } catch (Throwable e) {
      throw new BeanCreationException("Could not create or connect RTMClient with WSS Url: " + wssUrl, e);
    }
  }

  /**
   * Bean containing global Slack state, as returned by the RTM connection response.
   */
  @Bean
  @Scope("singleton")
  public RTMStartResponse getRtmState(@Qualifier("slackToken") String slackToken, Slack slack) {
    try {
      MethodsClient methods = slack.methods();
      return methods.rtmStart(
          RTMStartRequest.builder()
              .token(slackToken)
              .noUnreads(true)    // improves performance by avoiding to retrieve unread counts
              .mpimAware(false)   // multiparty IMs, when false, they'll be shown as groups.
              .build());
    } catch (Throwable e) {
      throw new BeanCreationException("Could not fetch RTM API WebSocket endpoint. Possibly 'slack.token' is incorrect.", e);
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
