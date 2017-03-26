package io.arivera.oss.jchatops.responders;

import com.github.seratch.jslack.api.model.Im;
import com.github.seratch.jslack.api.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Scope("singleton")
public class TagUserResponseProcessor implements ResponseProcessor {

  private final Map<String, Im> instantMessages;

  @Autowired
  public TagUserResponseProcessor(Map<String, Im> instantMessages) {
    this.instantMessages = instantMessages;
  }

  @Override
  public Response apply(Response response) {
    Message originalMessage = response.getOriginalMessage();
    Message message = response.getResponseMessage();

    boolean isPrivateMessage = instantMessages.containsKey(originalMessage.getChannel());
    if (!isPrivateMessage) {
      message.setText(String.format("<@%s>: %s", originalMessage.getUser(), message.getText()));
    }
    return response;
  }

}