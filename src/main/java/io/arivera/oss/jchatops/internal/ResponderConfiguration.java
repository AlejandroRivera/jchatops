package io.arivera.oss.jchatops.internal;

import io.arivera.oss.jchatops.responders.Responder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ResponderConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResponderConfiguration.class);

  @Autowired
  @Qualifier("rtm")
  Responder rtmResponder;

  @Autowired
  @Qualifier("web-api")
  Responder apiResponder;

  /**
   * Returns the Web API-based responder due to supporting more features than the the RTM payload (eg. Attachments).
   *
   * <p>Override this bean with RTM implementation if needed --  for example, if you have firewall rules that prevent
   * submitting WS Requests to Slack.
   *
   * TODO: Make ConditionalOnBean
   */
  @Bean
  @Primary
  public Responder getResponder(){
    return apiResponder;
  }

}
