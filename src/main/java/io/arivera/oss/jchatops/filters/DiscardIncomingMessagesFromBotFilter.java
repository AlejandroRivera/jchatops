package io.arivera.oss.jchatops.filters;

import io.arivera.oss.jchatops.MessageFilter;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Scope("prototype")
public class DiscardIncomingMessagesFromBotFilter extends MessageFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(DiscardIncomingMessagesFromBotFilter.class);

  private final User botUser;

  @Autowired
  public DiscardIncomingMessagesFromBotFilter(@Value("${jchatops.filters.tag_user:100}") int order,
                                              User botUser) {
    super(order);
    this.botUser = botUser;
  }

  @Override
  public Optional<Response> apply(Message message) {
    if (message.getUser().equals(botUser.getId())) {
      LOGGER.info("Message received is from this bot itself. It will be ignored.");
      return Optional.empty();
    }

    return getNextFilter().apply(message);
  }

}