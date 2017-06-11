package io.arivera.oss.jchatops.filters;

import io.arivera.oss.jchatops.MessageFilter;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Scope("singleton")
public class SameChannelResponseFilter extends MessageFilter {

  @Autowired
  public SameChannelResponseFilter(@Value("${jchatops.filters.same_channel:500}") int order) {
    super(order);
  }

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
