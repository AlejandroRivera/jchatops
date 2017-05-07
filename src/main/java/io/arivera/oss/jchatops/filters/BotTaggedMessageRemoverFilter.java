package io.arivera.oss.jchatops.filters;

import io.arivera.oss.jchatops.MessageFilter;
import io.arivera.oss.jchatops.MessageType;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Order(100)
@Component
@Scope("prototype")
public class BotTaggedMessageRemoverFilter extends MessageFilter {

  private final MessageType messageType;
  private final User bot;

  @Autowired
  public BotTaggedMessageRemoverFilter(MessageType messageType, User bot) {
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
