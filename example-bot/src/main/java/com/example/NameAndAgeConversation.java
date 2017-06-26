package com.example;

import io.arivera.oss.jchatops.ConversationData;
import io.arivera.oss.jchatops.MessageHandler;
import io.arivera.oss.jchatops.MessageType;
import io.arivera.oss.jchatops.responders.Response;

import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

@Service
public class NameAndAgeConversation {

  @MessageHandler(
      patterns = ".*(?<greeting>hello|hey|hi|yo).*",
      messageTypes = {MessageType.PRIVATE, MessageType.TAGGED})
  public Response hello(Response response, Matcher patternMatched,
                        @Qualifier("bot") User bot) {
    return response
        .message(String.format("%s back! I'm `%s`. What is your name?", patternMatched.group("greeting"), bot.getName()))
        .followingUpWith("getNameAndAskAge");
  }

  @MessageHandler(
      patterns = {"(?<name>\\w+)", "my name is (?<name>\\w+)", "(?<name>\\w+) is what they call me"},
      requiresConversation = true)
  public Response getNameAndAskAge(Response response, Matcher matcher,
                                   @Qualifier("messageGraph") User sender,
                                   ConversationData conversation) {
    String nameGiven = matcher.group("name");
    conversation.put("name", nameGiven);

    List<String> responseMsgs = new ArrayList<>(2);
    if (!sender.getName().equalsIgnoreCase(nameGiven)) {
      responseMsgs.add("Weird... your profile says your name is `" + sender.getName() + "`\n");
    }
    responseMsgs.add("How old are you?");
    return response
          .message(responseMsgs)
          .followingUpWith("getAgeAndConfirm");
  }

  @MessageHandler(patterns = "\\d+", requiresConversation = true)
  public Response getAgeAndConfirm(Message message, Response response, ConversationData conversation) {
      conversation.put("age", message.getText());
      return response
          .message(String.format("You are '%s' and you are %s years old. Did I get that right?",
              conversation.get("name"),
              conversation.get("age")))
          .followingUpWith("correct", "incorrect");
  }

  @MessageHandler(patterns = "(no|no way|nope|not at all|wrong|incorrect)", requiresConversation = true)
  public Response incorrect(Response response) {
    return response
        .message("Oops! Let's start over... What is your name?")
        .followingUpWith("getNameAndAskAge");
  }

  @MessageHandler(patterns = "(yes|yup|correct|perfect|indeed|great)", requiresConversation = true)
  public Response correct(Response response) {
    return response
        .resettingConversation()
        .message("Confirmed!");
  }

}
