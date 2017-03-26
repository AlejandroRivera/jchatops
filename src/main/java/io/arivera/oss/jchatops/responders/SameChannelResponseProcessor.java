package io.arivera.oss.jchatops.responders;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
public class SameChannelResponseProcessor implements ResponseProcessor {

  @Override
  public Response apply(Response response) {
    response.getResponseMessage().setChannel(response.getOriginalMessage().getChannel());
    return response;
  }

}
