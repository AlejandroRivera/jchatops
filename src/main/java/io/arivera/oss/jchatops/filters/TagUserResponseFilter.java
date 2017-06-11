package io.arivera.oss.jchatops.filters;

import io.arivera.oss.jchatops.MessageFilter;
import io.arivera.oss.jchatops.MessageType;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Scope("prototype")
public class TagUserResponseFilter extends MessageFilter {

  private final MessageType messageType;

  @Autowired
  public TagUserResponseFilter(@Value("${jchatops.filters.tag_user:700}") int order,
                               MessageType messageType) {
    super(order);
    this.messageType = messageType;
  }

  @Override
  public Optional<Response> apply(Message message) {
    return getNextFilter().apply(message)
        .map(response -> response
            .wrapSlackMessages(messageStream ->
                messageStream.map(msg -> {
                      if (messageType != MessageType.PRIVATE) {
                        msg.setText(String.format("<@%s>: %s", message.getUser(), msg.getText()));
                      }
                      return msg;
                    }
                )
            )
        );
  }

}