package io.arivera.oss.jchatops.filters;

import io.arivera.oss.jchatops.MessageFilter;
import io.arivera.oss.jchatops.MessageType;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Scope("prototype")
public class BotTaggedMessageRemoverFilter extends MessageFilter {

  private final MessageType messageType;
  private final User bot;

  @Autowired
  public BotTaggedMessageRemoverFilter(@Value("${jchatops.filters.unrecognized_command:300}") int order,
                                       MessageType messageType, User bot) {
    super(order);
    this.messageType = messageType;
    this.bot = bot;
  }

  @Override
  public Optional<Response> apply(Message message) {
    if (messageType == MessageType.TAGGED) {
      message.setText(message.getText().replaceAll("^\\s*<@" + bot.getId() + ">\\S*", "").trim());
    }

    return getNextFilter().apply(message);
  }
}
