package io.arivera.oss.jchatops.internal;

import com.github.seratch.jslack.api.rtm.RTMMessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component("async_handler")
@Scope("singleton")
public class MessageHandlerAsync implements RTMMessageHandler {

  private final RTMMessageHandler realHandler;

  @Autowired
  public MessageHandlerAsync(@Qualifier("sync_handler") RTMMessageHandler realHandler) {
    this.realHandler = realHandler;
  }

  @Async
  @Override
  public void handle(String message) {
    realHandler.handle(message);
  }

}
