package io.arivera.oss.jchatops.responders;

import io.arivera.oss.jchatops.MessageHandler;
import io.arivera.oss.jchatops.ResponseSupplier;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CancelConversationHandlers {

  public static final String RESET_BEAN_NAME = "resetConversation";
  public static final String RESET_KEY_PHRASE = "forget about it";

  /**
   * Message handler for when a conversation needs to be cancelled or reset.
   */
  @Bean(RESET_BEAN_NAME)
  @MessageHandler(patterns = RESET_KEY_PHRASE, requiresConversation = true)
  public ResponseSupplier cancelConversation(Response response) {
    return () -> response
        .resettingConversation()
        .message("Ok, let's start over... how can I help?");
  }

}
