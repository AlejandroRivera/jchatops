package io.arivera.oss.jchatops.responders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.Arrays;
import java.util.List;

@Configuration
public class RespondersConfiguration {

  @Bean
  @Scope("singleton")
  @Autowired
  public List<ResponseProcessor> getResponseProcessors(SameChannelResponseProcessor sameChannel,
                                                       TagUserResponseProcessor tagUser) {
    return Arrays.asList(sameChannel, tagUser);
  }

}
