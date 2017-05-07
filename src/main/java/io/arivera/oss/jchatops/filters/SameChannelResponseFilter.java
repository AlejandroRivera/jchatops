package io.arivera.oss.jchatops.filters;

import io.arivera.oss.jchatops.MessageFilter;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Message;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 *
 */
@Order(200)
@Component
@Scope("singleton")
public class SameChannelResponseFilter extends MessageFilter {

  @Override
  public Optional<Response> apply(Message message) {
    return getNextFilter().apply(message)
        .map(res -> {
              res.getResponseMessage().setChannel(message.getChannel());
              return res;
            }
        );
  }

}
