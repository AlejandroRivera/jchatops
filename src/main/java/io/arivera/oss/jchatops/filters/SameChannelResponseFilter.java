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
 * This filter ensures the message to be submitted is sent back in the same channel.
 * <p>
 * This is only applicable if the message doesn't already specify a destination channel.
 */
@Component
@Scope("singleton")
public class SameChannelResponseFilter extends MessageFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(SameChannelResponseFilter.class);

  @Autowired
  public SameChannelResponseFilter(@Value("${jchatops.filters.same_channel:500}") int order) {
    super(order);
  }

  @Override
  public Optional<Response> apply(Message message) {
    return getNextFilter().apply(message)
        .map(response -> response.wrapSlackMessages(
            (msgStream) -> msgStream
                .map(msg -> {
                  if (msg.getChannel() == null) {
                    String channel = message.getChannel();
                    LOGGER.debug("Channel to deliver message will be the same as the original incoming message: {}", channel);
                    msg.setChannel(channel);
                  }
                  return msg;
                })
            )
        );
  }

}
