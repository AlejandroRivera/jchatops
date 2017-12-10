package io.arivera.oss.jchatops.filters;

import io.arivera.oss.jchatops.MessageFilter;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * This filter ensures the message to be submitted is sent back in the same thread as the message received (if any)
 * <p>
 * This is only applicable if the message doesn't already specify a parent Thread.
 */
@Component
@Scope("singleton")
public class ThreadedResponseFilter extends MessageFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ThreadedResponseFilter.class);

  @Autowired
  public ThreadedResponseFilter(@Value("${jchatops.filters.same_channel:600}") int order) {
    super(order);
  }

  @Override
  public Optional<Response> apply(Message incomingMessage) {
    boolean isThreadedMessage = Optional.ofNullable(incomingMessage.getThreadTs())
        .filter(s -> !s.equalsIgnoreCase(incomingMessage.getTs()))
        .isPresent();

    return getNextFilter().apply(incomingMessage)
        .map(response -> response.wrapSlackMessages(
            (msgStream) -> msgStream
                .map(msg -> {
                  Boolean respondInThread = response.shouldRespondInThread().orElse(isThreadedMessage);
                  String parentThread = Optional.ofNullable(incomingMessage.getThreadTs()).orElse(incomingMessage.getTs());

                  if (msg.getThreadTs() == null && respondInThread) {
                    LOGGER.debug("Response message(s) will be delivered in the same conversation thread.");
                    msg.setThreadTs(parentThread);
                  }
                  return msg;
                })
            )
        );
  }

}
