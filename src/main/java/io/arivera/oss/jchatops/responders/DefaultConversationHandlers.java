package io.arivera.oss.jchatops.responders;

import io.arivera.oss.jchatops.MessageHandler;
import io.arivera.oss.jchatops.ResponseSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DefaultConversationHandlers {

  public static final String UNEXPECTED_MSG_BEAN_NAME = "unexpectedConversationResponse";
  public static final String RESET_BEAN_NAME = "resetConversation";

  public static final String RESET_KEY_PHRASE = "forget about it";

  @Bean(UNEXPECTED_MSG_BEAN_NAME)
  @MessageHandler(patterns = ".*", requiresConversation = true)
  public ResponseSupplier unexpectedConversationResponse(Response response) {
    return () -> response.message("Sorry, I did not understand that. Say '" + RESET_KEY_PHRASE + "' to reset this conversation.");
  }

  @Bean(RESET_BEAN_NAME)
  @MessageHandler(patterns = RESET_KEY_PHRASE, requiresConversation = true)
  public ResponseSupplier cancelConversationResponse(Response response) {
    return () -> response
        .resettingConversation()
        .message("Let's start over... how can i help?");
  }



}
