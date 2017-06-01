package io.arivera.oss.jchatops;

import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Message;
import org.springframework.core.Ordered;

import java.util.Optional;

public abstract class MessageFilter implements Ordered {

  private final int order;

  private MessageFilter nextFilter;

  public MessageFilter(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return order;
  }

  public void setNextFilter(MessageFilter nextFilter) {
    this.nextFilter = nextFilter;
  }

  public final MessageFilter getNextFilter() {
    return nextFilter;
  }

  public abstract Optional<Response> apply(Message message);

}
