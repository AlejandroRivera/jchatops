package io.arivera.oss.jchatops.filters;

import io.arivera.oss.jchatops.MessageFilter;
import io.arivera.oss.jchatops.MessageType;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Scope("prototype")
public class UnrecognizedCommandFilter extends MessageFilter {

  public static final String RESET_KEY_PHRASE = "forget about it";

  private static final Logger LOGGER = LoggerFactory.getLogger(UnrecognizedCommandFilter.class);

  private final Response response;
  private final MessageType messageType;

  @Autowired
  public UnrecognizedCommandFilter(@Value("${jchatops.filters.unrecognized_command:900}") int order,
                                   Response response, MessageType messageType) {
    super(order);
    this.response = response;
    this.messageType = messageType;
  }

  @Override
  public Optional<Response> apply(Message message) {
    Optional<Response> maybeResponse = getNextFilter().apply(message);
    if (maybeResponse.isPresent() || messageType == MessageType.PUBLIC) {
      return maybeResponse;
    } else {
      LOGGER.info("Received message did not match any message handlers.");
      response.message("Sorry, I did not understand that. Say '" + RESET_KEY_PHRASE + "' to reset this conversation.");
      return Optional.of(response);
    }
  }

  @Override
  public int getOrder() {
    return 0;
  }
}
