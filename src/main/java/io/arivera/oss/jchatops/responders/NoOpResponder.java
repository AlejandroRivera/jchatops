package io.arivera.oss.jchatops.responders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
public class NoOpResponder implements Responder {

  private static final Logger LOGGER = LoggerFactory.getLogger(NoOpResponder.class);

  @Override
  public void respondWith(Response response) {
    LOGGER.debug("No message will be submitted as response.");
  }

}
