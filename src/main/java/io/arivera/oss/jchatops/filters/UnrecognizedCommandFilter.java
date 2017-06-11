package io.arivera.oss.jchatops.filters;

import io.arivera.oss.jchatops.MessageFilter;
import io.arivera.oss.jchatops.MessageType;
import io.arivera.oss.jchatops.internal.ConversationContext;
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

  public static final String UNRECOGNIZED_COMMAND_MESSAGE = "Sorry, I did not understand that.";
  public static final String RESET_KEY_PHRASE = "forget about it";
  public static final String CONVERSATION_INSTRUCTIONS = " Say '" + RESET_KEY_PHRASE + "' to reset this conversation.";

  private static final Logger LOGGER = LoggerFactory.getLogger(UnrecognizedCommandFilter.class);

  private final Response response;
  private final MessageType messageType;
  private final Optional<ConversationContext> conversationContext;

  @Autowired
  public UnrecognizedCommandFilter(@Value("${jchatops.filters.unrecognized_command:900}") int order,
                                   Response response,
                                   MessageType messageType,
                                   Optional<ConversationContext> conversationContext) {
    super(order);
    this.response = response;
    this.messageType = messageType;
    this.conversationContext = conversationContext;
  }

  @Override
  public Optional<Response> apply(Message message) {
    Optional<Response> maybeResponse = getNextFilter().apply(message);
    if (maybeResponse.isPresent() || messageType == MessageType.PUBLIC) {
      return maybeResponse;
    } else {
      LOGGER.info("Received message did not match any message handlers.");
      String msg = UNRECOGNIZED_COMMAND_MESSAGE;
      if (conversationContext.isPresent()) {
        msg += CONVERSATION_INSTRUCTIONS;
      }
      response.message(msg);
      return Optional.of(response);
    }
  }

}
