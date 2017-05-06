package io.arivera.oss.jchatops.internal;

import com.github.seratch.jslack.api.rtm.RTMClient;
import com.github.seratch.jslack.api.rtm.RTMMessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@EnableAsync
@Configuration
public class MessageHandlerConfiguration {

  private final RTMClient rtmClient;
  private final RTMMessageHandler messageHandler;

  @Autowired
  public MessageHandlerConfiguration(RTMClient rtmClient,
                              @Value("${jchatops.message_handler.async:true}") boolean async,
                              @Qualifier("sync_handler") RTMMessageHandler messageHandler,
                              @Qualifier("async_handler") RTMMessageHandler asyncMessageHandler) {
    this.rtmClient = rtmClient;
    this.messageHandler = async ? asyncMessageHandler : messageHandler;
  }

  @PostConstruct
  public void init() {
    rtmClient.addMessageHandler(messageHandler);
  }

  @PreDestroy
  public void finish() {
    rtmClient.removeMessageHandler(messageHandler);
  }
}
