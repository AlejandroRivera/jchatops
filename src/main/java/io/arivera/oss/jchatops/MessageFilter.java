package io.arivera.oss.jchatops;

import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Message;

import java.util.Optional;

public abstract class MessageFilter {

  private MessageFilter nextFilter;

  public MessageFilter() {
  }

  public void setNextFilter(MessageFilter nextFilter) {
    this.nextFilter = nextFilter;
  }

  public final MessageFilter getNextFilter() {
    return nextFilter;
  }

  public abstract Optional<Response> apply(Message message);

}
