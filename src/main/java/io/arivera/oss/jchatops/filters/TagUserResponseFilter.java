package io.arivera.oss.jchatops.filters;

import io.arivera.oss.jchatops.MessageFilter;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Im;
import com.github.seratch.jslack.api.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Order(300)
@Component
@Scope("prototype")
public class TagUserResponseFilter extends MessageFilter {

  private final Map<String, Im> instantMessages;

  @Autowired
  public TagUserResponseFilter(Map<String, Im> instantMessages) {
    this.instantMessages = instantMessages;
  }

  @Override
  public Optional<Response> apply(Message message) {
    return getNextFilter().apply(message)
        .map(response -> {
              Message responseMessage = response.getResponseMessage();

              boolean isPrivateMessage = instantMessages.containsKey(message.getChannel());
              if (!isPrivateMessage) {
                responseMessage.setText(String.format("<@%s>: %s", message.getUser(), responseMessage.getText()));
              }
              return response;
            }
        );
  }

}