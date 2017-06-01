package com.example;

import io.arivera.oss.jchatops.ConversationData;
import io.arivera.oss.jchatops.MessageHandler;
import io.arivera.oss.jchatops.MessageType;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service
public class NameAndAgeConversation {

  private static final Logger LOGGER = LoggerFactory.getLogger(NameAndAgeConversation.class);

  @Bean
  @MessageHandler(patterns = "hello", messageTypes = {MessageType.PRIVATE, MessageType.TAGGED})
  @Secured("ROLE_ADMIN")
  public Response hello(Message message, Response response) {
    return response
        .message("hey there! What is your name?")
        .followingUpWith("getNameAndAskAge");
  }

  @Bean
  @MessageHandler(
      patterns = {
          "(?<name>\\w+)",
          "my name is (?<name>\\w+)",
          "(?<name>\\w+) is what they call me"
      },
      requiresConversation = true)
  public Response getNameAndAskAge(Response response, Matcher matcher, ConversationData conversation) {
      conversation.put("name", matcher.group("name"));
      return response
          .message("How old are you?")
          .followingUpWith("getAgeAndConfirm");
  }

  @Bean
  @MessageHandler(patterns = "\\d+", requiresConversation = true)
  public Response getAgeAndConfirm(Message message, Response response, ConversationData conversation) {
      conversation.put("age", message.getText());
      return response
          .message(String.format("You are '%s' and you are %s years old. Did I get that right?",
              conversation.get("name"),
              conversation.get("age")))
          .followingUpWith("correct", "incorrect");
  }

  @Bean
  @MessageHandler(patterns = "(no|no way|nope|not at all|wrong|incorrect)", requiresConversation = true)
  public Response incorrect(Response response) {
    return response
        .message("Oops! Let's start over... What is your name?")
        .followingUpWith("getNameAndAskAge");
  }

  @Bean
  @MessageHandler(patterns = "(yes|yup|correct|perfect|indeed|great)", requiresConversation = true)
  public Response correct(Response response) {
    return response
        .resettingConversation()
        .message("Confirmed!");
  }

}
